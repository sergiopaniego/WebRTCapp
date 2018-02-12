package com.sergiopaniegoblanco.webrtcexampleapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;


import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private static final int MY_PERMISSIONS_REQUEST = 102;

    private PeerConnection localPeer;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoRenderer remoteRenderer;
    private AudioTrack localAudioTrack;
    private VideoTrack localVideoTrack;
    private WebSocket webSocket;
    private CustomWebSocketAdapter webSocketAdapter;
    private VideoRenderer localRenderer;

    @BindView(R.id.views_container)
    LinearLayout views_container;
    @BindView(R.id.start_finish_call)
    Button start_finish_call;
    @BindView(R.id.session_name)
    EditText session_name;
    @BindView(R.id.participant_name)
    EditText participant_name;
    @BindView(R.id.socketAddress)
    EditText socket_address;
    @BindView(R.id.local_gl_surface_view)
    SurfaceViewRenderer localVideoView;
    @BindView(R.id.main_participant)
    TextView main_participant;
    @BindView(R.id.peer_container)
    FrameLayout peer_container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        askForPermissions();
        ButterKnife.bind(this);
        initViews();
    }

    public void askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    public void initViews() {
        localVideoView.setMirror(true);
        EglBase rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
    }

    public void start(View view) {
        if (start_finish_call.getText().equals(getResources().getString(R.string.hang_up))) {
            hangup();
            return;
        }
        start_finish_call.setText(getResources().getString(R.string.hang_up));
        socket_address.setEnabled(false);
        socket_address.setFocusable(false);
        session_name.setEnabled(false);
        session_name.setFocusable(false);
        participant_name.setEnabled(false);
        participant_name.setFocusable(false);

        PeerConnectionFactory.initializeAndroidGlobals(this, true);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = new PeerConnectionFactory(options);

        VideoCapturer videoGrabberAndroid = createVideoGrabber();
        MediaConstraints constraints = new MediaConstraints();

        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoGrabberAndroid);
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        videoGrabberAndroid.startCapture(1000, 1000, 30);

        localRenderer = new VideoRenderer(localVideoView);
        localVideoTrack.addRenderer(localRenderer);

        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        createLocalPeerConnection(sdpConstraints);
        createLocalSocket();
    }

    public void createLocalPeerConnection(MediaConstraints sdpConstraints) {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = new PeerConnection.IceServer("stun:stun.l.google.com:19302");
        iceServers.add(iceServer);

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, sdpConstraints, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, String> iceCandidateParams = new HashMap<>();
                iceCandidateParams.put("sdpMid", iceCandidate.sdpMid);
                iceCandidateParams.put("sdpMLineIndex", Integer.toString(iceCandidate.sdpMLineIndex));
                iceCandidateParams.put("candidate", iceCandidate.sdp);
                if (webSocketAdapter.getUserId() != null) {
                    iceCandidateParams.put("endpointName", webSocketAdapter.getUserId());
                    webSocketAdapter.sendJson(webSocket, "onIceCandidate", iceCandidateParams);
                } else {
                    webSocketAdapter.addIceCandidate(iceCandidateParams);
                }
            }
        });
    }

    public void createLocalSocket() {
        main_participant.setText(participant_name.getText().toString());
        main_participant.setPadding(20, 3, 20, 3);
        new WebSocketTask().execute(this);
    }

    class WebSocketTask extends AsyncTask<MainActivity, Void, Void> {

        protected Void doInBackground(MainActivity... parameters) {

            try {
                WebSocketFactory factory = new WebSocketFactory();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, new java.security.SecureRandom());
                factory.setSSLContext(sslContext);
                webSocket = new WebSocketFactory().createSocket(getSocketAddress());
                webSocketAdapter = new CustomWebSocketAdapter(parameters[0], localPeer, session_name.getText().toString(), participant_name.getText().toString(), views_container);
                webSocket.addListener(webSocketAdapter);
                webSocket.connect();
            } catch (IOException | KeyManagementException | WebSocketException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
        private String getSocketAddress() {
            String baseAddress = socket_address.getText().toString();
            String secureWebSocketPrefix = "wss://";
            String insecureWebSocketPrefix = "ws://";
            if (baseAddress.split(secureWebSocketPrefix).length == 1 && baseAddress.split(insecureWebSocketPrefix).length == 1) {
                baseAddress = secureWebSocketPrefix.concat(baseAddress);
            }
            String portSuffix = ":8443";
            if (baseAddress.split(portSuffix).length == 1 && !baseAddress.regionMatches(true, baseAddress.length() - portSuffix.length(), portSuffix, 0, portSuffix.length())) {
                baseAddress = baseAddress.concat(portSuffix);
            }
            String roomSuffix = "/room";
            if (!baseAddress.regionMatches(true, baseAddress.length() - roomSuffix.length(), roomSuffix, 0, roomSuffix.length())) {
                baseAddress = baseAddress.concat(roomSuffix);
            }
            return baseAddress;
        }

        protected void onProgressUpdate(Void... progress) {
            Log.i(TAG,"PROGRESS " + Arrays.toString(progress));
        }

        protected void onPostExecute(Void results) {
            MediaConstraints sdpConstraints = new MediaConstraints();
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

            MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
            stream.addTrack(localAudioTrack);
            stream.addTrack(localVideoTrack);
            localPeer.addStream(stream);

            createLocalOffer(sdpConstraints);
        }
    }

    /* Trust All Certificates */
    final TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain,
                                       final String authType) throws CertificateException {
            Log.i(TAG,": authType: " + String.valueOf(authType));
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain,
                                       final String authType) throws CertificateException {
            Log.i(TAG,": authType: " + String.valueOf(authType));
        }
    }};

    public void createLocalOffer(MediaConstraints sdpConstraints) {

        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Map<String, String> localOfferParams = new HashMap<>();
                localOfferParams.put("audioOnly", "false");
                localOfferParams.put("doLoopback", "false");
                localOfferParams.put("sdpOffer", sessionDescription.description);
                if (webSocketAdapter.getId() > 1) {
                    webSocketAdapter.sendJson(webSocket, "publishVideo", localOfferParams);
                } else {
                    webSocketAdapter.setLocalOfferParams(localOfferParams);
                }
            }
        }, sdpConstraints);
    }

    public void createRemotePeerConnection(RemoteParticipant remoteParticipant) {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = new PeerConnection.IceServer("stun:stun.l.google.com:19302");
        iceServers.add(iceServer);

        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        PeerConnection remotePeer = peerConnectionFactory.createPeerConnection(iceServers, sdpConstraints, new CustomPeerConnectionObserver("remotePeerCreation", remoteParticipant) {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, String> iceCandidateParams = new HashMap<>();
                iceCandidateParams.put("sdpMid", iceCandidate.sdpMid);
                iceCandidateParams.put("sdpMLineIndex", Integer.toString(iceCandidate.sdpMLineIndex));
                iceCandidateParams.put("candidate", iceCandidate.sdp);
                iceCandidateParams.put("endpointName", getRemoteParticipant().getId());
                webSocketAdapter.sendJson(webSocket, "onIceCandidate", iceCandidateParams);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream, getRemoteParticipant());
            }
        });
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("105");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        remotePeer.addStream(stream);
        remoteParticipant.setPeerConnection(remotePeer);
    }

    public VideoCapturer createVideoGrabber() {
        VideoCapturer videoCapturer;
        videoCapturer = createCameraGrabber(new Camera1Enumerator(false));
        return videoCapturer;
    }

    public VideoCapturer createCameraGrabber(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void gotRemoteStream(MediaStream stream, final RemoteParticipant remoteParticipant) {
        final VideoTrack videoTrack = stream.videoTracks.getFirst();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteRenderer = new VideoRenderer(remoteParticipant.getVideoView());
                    remoteParticipant.getVideoView().setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);
                    MediaStream stream = peerConnectionFactory.createLocalMediaStream("105");
                    stream.addTrack(localAudioTrack);
                    stream.addTrack(localVideoTrack);
                    remoteParticipant.getPeerConnection().removeStream(stream);
                    remoteParticipant.getPeerConnection().addStream(stream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setRemoteParticipantName(String name, RemoteParticipant remoteParticipant) {
        remoteParticipant.getParticipantNameText().setText(name);
        remoteParticipant.getParticipantNameText().setPadding(20, 3, 20, 3);
    }

    @Override
    protected void onDestroy() {
        hangup();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        hangup();
        super.onBackPressed();
    }

    public void hangup() {
        if (webSocketAdapter != null && localPeer != null) {
            webSocketAdapter.sendJson(webSocket, "leaveRoom",  new HashMap<String, String>());
            localPeer.close();
            localVideoTrack.removeRenderer(localRenderer);
            localVideoView.clearImage();

            Map<String, RemoteParticipant> participants = webSocketAdapter.getParticipants();
            for (RemoteParticipant remoteParticipant : participants.values()) {
                remoteParticipant.getPeerConnection().close();
                views_container.removeView(remoteParticipant.getView());

            }
            localPeer = null;
            start_finish_call.setText(getResources().getString(R.string.start_button));
            socket_address.setEnabled(true);
            socket_address.setFocusableInTouchMode(true);
            session_name.setEnabled(true);
            session_name.setFocusableInTouchMode(true);
            participant_name.setEnabled(true);
            participant_name.setFocusableInTouchMode(true);
            main_participant.setText(null);
            main_participant.setPadding(0, 0, 0, 0);
        }
    }
}