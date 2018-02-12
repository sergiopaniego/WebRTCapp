package com.sergiopaniegoblanco.webrtcexampleapp;

import android.view.View;
import android.widget.TextView;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * Created by sergiopaniegoblanco on 08/02/2018.
 */

public class RemoteParticipant {

    private String id;
    private MediaStream mediaStream;
    private String userName;
    private PeerConnection peerConnection;
    private AudioTrack audioTrack;
    private VideoTrack videoTrack;
    private SurfaceViewRenderer videoView;
    private View view;
    private TextView participantNameText;

    public RemoteParticipant(String id, MediaStream mediaStream, String userName, PeerConnection peerConnection, AudioTrack audioTrack, VideoTrack videoTrack, View view, TextView participantNameText) {
        this.id = id;
        this.mediaStream = mediaStream;
        this.userName = userName;
        this.peerConnection = peerConnection;
        this.audioTrack = audioTrack;
        this.videoTrack = videoTrack;
        this.view = view;
        this.participantNameText = participantNameText;
    }

    public RemoteParticipant() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MediaStream getMediaStream() {
        return mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public VideoTrack getVideoTrack() {
        return videoTrack;
    }

    public void setVideoTrack(VideoTrack videoTrack) {
        this.videoTrack = videoTrack;
    }

    public SurfaceViewRenderer getVideoView() {
        return videoView;
    }

    public void setVideoView(SurfaceViewRenderer videoView) {
        this.videoView = videoView;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public TextView getParticipantNameText() {
        return participantNameText;
    }

    public void setParticipantNameText(TextView participantNameText) {
        this.participantNameText = participantNameText;
    }
}
