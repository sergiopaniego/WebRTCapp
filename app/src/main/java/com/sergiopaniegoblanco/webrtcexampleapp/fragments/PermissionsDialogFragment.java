package com.sergiopaniegoblanco.webrtcexampleapp.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.sergiopaniegoblanco.webrtcexampleapp.VideoConferenceActivity;
import com.sergiopaniegoblanco.webrtcexampleapp.R;

/**
 * Created by sergiopaniegoblanco on 18/02/2018.
 */

public class PermissionsDialogFragment extends DialogFragment {

    private static final String TAG = "PermissionsDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.permissions_dialog_title);
        builder.setMessage(R.string.no_permissions_granted)
                .setPositiveButton(R.string.accept_permissions_dialog, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((VideoConferenceActivity)getActivity()).askForPermissions();
                    }
                })
                .setNegativeButton(R.string.cancel_dialog, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "User cancelled Permissions Dialog");
                    }
                });
        return builder.create();
    }
}