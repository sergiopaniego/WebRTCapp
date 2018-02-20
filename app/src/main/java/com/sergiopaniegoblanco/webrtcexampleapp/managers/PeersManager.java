package com.sergiopaniegoblanco.webrtcexampleapp.managers;

import android.widget.LinearLayout;

import com.neovisionaries.ws.client.WebSocket;
import com.sergiopaniegoblanco.webrtcexampleapp.VideoConferenceActivity;
import com.sergiopaniegoblanco.webrtcexampleapp.RemoteParticipant;
import com.sergiopaniegoblanco.webrtcexampleapp.listeners.CustomWebSocketListener;
import com.sergiopaniegoblanco.webrtcexampleapp.observers.CustomPeerConnectionObserver;
import com.sergiopaniegoblanco.webrtcexampleapp.observers.CustomSdpObserver;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sergiopaniegoblanco on 18/02/2018.
 */

public class PeersManager {

    private PeerConnection localPeer;
    private PeerConnectionFactory peerConnectionFactory;
    private CustomWebSocketListener webSocketAdapter;
    private WebSocket webSocket;
    private LinearLayout views_container;
    private AudioTrack localAudioTrack;
    private VideoTrack localVideoTrack;
    private VideoRenderer localRenderer;
    private SurfaceViewRenderer localVideoView;
    private VideoCapturer videoGrabberAndroid;
    private VideoConferenceActivity activity;

    public PeersManager(VideoConferenceActivity activity, LinearLayout views_container, SurfaceViewRenderer localVideoView) {
        this.views_container = views_container;
        this.localVideoView = localVideoView;
        this.activity = activity;
    }

    public PeerConnection getLocalPeer() {
        return localPeer;
    }

    public AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    public VideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public CustomWebSocketListener getWebSocketAdapter() {
        return webSocketAdapter;
    }

    public void setWebSocketAdapter(CustomWebSocketListener webSocketAdapter) {
        this.webSocketAdapter = webSocketAdapter;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void start() {
        PeerConnectionFactory.initializeAndroidGlobals(activity, true);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = new PeerConnectionFactory(options);

        videoGrabberAndroid = createVideoGrabber();
        MediaConstraints constraints = new MediaConstraints();

        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoGrabberAndroid);
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        if (videoGrabberAndroid != null) {
            videoGrabberAndroid.startCapture(1000, 1000, 30);
        }

        localRenderer = new VideoRenderer(localVideoView);
        localVideoTrack.addRenderer(localRenderer);

        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        createLocalPeerConnection(sdpConstraints);
    }

    private VideoCapturer createVideoGrabber() {
        VideoCapturer videoCapturer;
        videoCapturer = createCameraGrabber(new Camera1Enumerator(false));
        return videoCapturer;
    }

    private VideoCapturer createCameraGrabber(CameraEnumerator enumerator) {
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

    private void createLocalPeerConnection(MediaConstraints sdpConstraints) {
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
                activity.gotRemoteStream(mediaStream, getRemoteParticipant());
            }
        });
        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("105");
        mediaStream.addTrack(localAudioTrack);
        mediaStream.addTrack(localVideoTrack);
        remotePeer.addStream(mediaStream);
        remoteParticipant.setPeerConnection(remotePeer);
    }


    public void hangup() {
        if (webSocketAdapter != null && localPeer != null) {
            webSocketAdapter.sendJson(webSocket, "leaveRoom", new HashMap<String, String>());
            webSocket.disconnect();
            localPeer.close();
            Map<String, RemoteParticipant> participants = webSocketAdapter.getParticipants();
            for (RemoteParticipant remoteParticipant : participants.values()) {
                remoteParticipant.getPeerConnection().close();
                views_container.removeView(remoteParticipant.getView());

            }
        }
        if (localVideoTrack != null) {
            localVideoTrack.removeRenderer(localRenderer);
            localVideoView.clearImage();
            videoGrabberAndroid.dispose();
        }
    }
}
