package com.sergiopaniegoblanco.webrtcexampleapp;

import android.Manifest;
import android.support.v4.app.DialogFragment;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sergiopaniegoblanco.webrtcexampleapp.fragments.PermissionsDialogFragment;
import com.sergiopaniegoblanco.webrtcexampleapp.managers.PeersManager;
import com.sergiopaniegoblanco.webrtcexampleapp.tasks.WebSocketTask;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoConferenceActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private static final int MY_PERMISSIONS_REQUEST = 102;

    private VideoRenderer remoteRenderer;
    private PeersManager peersManager;
    private WebSocketTask webSocketTask;

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        askForPermissions();
        ButterKnife.bind(this);
        Random random = new Random();
        int randomIndex = random.nextInt(100);
        participant_name.setText(participant_name.getText().append(String.valueOf(randomIndex)));
        this.peersManager = new PeersManager(this, views_container, localVideoView);
        initViews();
    }

    public LinearLayout getViewsContainer() {
        return views_container;
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
        if (arePermissionGranted()) {
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
            peersManager.start();
            createLocalSocket();
        } else {
            DialogFragment permissionsFragment = new PermissionsDialogFragment();
            permissionsFragment.show(getSupportFragmentManager(), "Permissions Fragment");
        }
    }

    private boolean arePermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_DENIED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED);
    }

    public void createLocalSocket() {
        main_participant.setText(participant_name.getText().toString());
        main_participant.setPadding(20, 3, 20, 3);
        String sessionName = session_name.getText().toString();
        String participantName = participant_name.getText().toString();
        String socketAddress = socket_address.getText().toString();
        webSocketTask = (WebSocketTask) new WebSocketTask(this, peersManager, sessionName, participantName, socketAddress).execute(this);
    }

    public void gotRemoteStream(MediaStream stream, final RemoteParticipant remoteParticipant) {
        final VideoTrack videoTrack = stream.videoTracks.getFirst();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteRenderer = new VideoRenderer(remoteParticipant.getVideoView());
                remoteParticipant.getVideoView().setVisibility(View.VISIBLE);
                videoTrack.addRenderer(remoteRenderer);
                MediaStream mediaStream = peersManager.getPeerConnectionFactory().createLocalMediaStream("105");
                remoteParticipant.setMediaStream(mediaStream);
                mediaStream.addTrack(peersManager.getLocalAudioTrack());
                mediaStream.addTrack(peersManager.getLocalVideoTrack());
                remoteParticipant.getPeerConnection().removeStream(mediaStream);
                remoteParticipant.getPeerConnection().addStream(mediaStream);
            }
        });
    }

    public void setRemoteParticipantName(String name, RemoteParticipant remoteParticipant) {
        remoteParticipant.getParticipantNameText().setText(name);
        remoteParticipant.getParticipantNameText().setPadding(20, 3, 20, 3);
    }

    public void hangup() {
        webSocketTask.setCancelled(true);
        peersManager.hangup();
        localVideoView.release();
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

    @Override
    protected void onStop() {
        hangup();
        super.onStop();
    }
}