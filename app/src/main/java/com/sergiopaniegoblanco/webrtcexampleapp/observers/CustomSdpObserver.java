package com.sergiopaniegoblanco.webrtcexampleapp.observers;

import android.util.Log;

import com.sergiopaniegoblanco.webrtcexampleapp.RemoteParticipant;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by sergiopaniegoblanco on 30/09/2017.
 */

public class CustomSdpObserver implements SdpObserver {


    private String tag = this.getClass().getCanonicalName();
    private RemoteParticipant remoteParticipant;

    public CustomSdpObserver(String logTag) {
        this.tag = this.tag + " " + logTag;
    }

    public CustomSdpObserver(String logTag, RemoteParticipant remoteParticipant) {
        this.tag = this.tag + " " + logTag;
        this.remoteParticipant = remoteParticipant;
    }

    public RemoteParticipant getRemoteParticipant() {
        return remoteParticipant;
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
    }

    @Override
    public void onSetSuccess() {
        Log.d(tag, "onSetSuccess() called");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
    }

}