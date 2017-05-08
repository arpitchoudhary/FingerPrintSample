package com.arpit.fingerprintsam;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by arpit on 4/4/17.
 */

public class FingerPrintDialogHelper extends DialogFragment implements FingerPrintHelper.FingerPrintAuthCallBack {

    private FingerPrintHelper fingerPrintHelper;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerPrintManager;

    private Button cancelButton;
    private ImageView touchIcon;
    private TextView headTextView;
    private MainActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(android.app.DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.sign_in));
        View view = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        touchIcon = (ImageView) view.findViewById(R.id.fingerprint_icon);
        headTextView = (TextView) view.findViewById(R.id.fingerprint_description);


        fingerPrintHelper = new FingerPrintHelper(fingerPrintManager, cryptoObject, touchIcon, headTextView, this);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) getActivity();
    }

    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        this.cryptoObject = cryptoObject;
    }

    public void setFingerPrintManager(FingerprintManager fingerPrintManager) {
        this.fingerPrintManager = fingerPrintManager;
    }


    @Override
    public void onResume() {
        super.onResume();
        fingerPrintHelper.startListeningForFingerTouch();
    }

    @Override
    public void onPause() {
        super.onPause();
        fingerPrintHelper.stopListeningForFingerTouch();
    }

    @Override
    public void onAuthenticated() {
        activity.beginEncryption(true);
        dismiss();
    }

    @Override
    public void onAuthenticationFailed() {
        activity.beginEncryption(false);
        dismiss();
    }
}
