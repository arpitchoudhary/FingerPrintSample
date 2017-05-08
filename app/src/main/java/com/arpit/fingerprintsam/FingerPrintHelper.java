package com.arpit.fingerprintsam;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by arpit on 4/4/17.
 */
public class FingerPrintHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 1000;
    private static final long SUCCESS_DELAY_MILLIS = 1000;

    private final FingerprintManager manager;
    private final FingerprintManager.CryptoObject cryptoObject;
    private final ImageView fingerPrintIcon;
    private final TextView headerTextView;
    private final FingerPrintAuthCallBack callback;
    private CancellationSignal cancellationSignal;
    private boolean selfCancelled;

    public FingerPrintHelper(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject,
                             ImageView fingerPrintIcon, TextView headerTextView, FingerPrintAuthCallBack callback) {

        this.manager = manager;
        this.cryptoObject = cryptoObject;
        this.fingerPrintIcon = fingerPrintIcon;
        this.headerTextView = headerTextView;
        this.callback = callback;
    }


    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        if (!selfCancelled) {
            showError(errString);
            fingerPrintIcon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onAuthenticationFailed();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        headerTextView.removeCallbacks(resetHeaderTextRunnable);
        fingerPrintIcon.setImageResource(R.drawable.ic_fingerprint_success);
        headerTextView.setTextColor(
                headerTextView.getResources().getColor(R.color.success_color, null));
        headerTextView.setText(
                headerTextView.getResources().getString(R.string.fingerprint_success));
        fingerPrintIcon.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onAuthenticated();
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(fingerPrintIcon.getResources().getString(
                R.string.fingerprint_not_recognized));
    }

    /**
     * method that will initiate the fingerprint process
     */
    public void startListeningForFingerTouch() {

        cancellationSignal = new CancellationSignal();
        selfCancelled = false;
        try {
            manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
        } catch (SecurityException sce) {
            // do we need to do anything here ??
        }

    }

    /**
     * method to stop listening for the finger touch
     */
    public void stopListeningForFingerTouch() {

        if (cancellationSignal != null) {
            selfCancelled = true;
            cancellationSignal.cancel();
            cancellationSignal = null;
        }

    }


    private void showError(CharSequence error) {
        fingerPrintIcon.setImageResource(R.drawable.ic_fingerprint_error);
        headerTextView.setText(error);
        headerTextView.setTextColor(
                headerTextView.getResources().getColor(R.color.warning_color, null));
        headerTextView.removeCallbacks(resetHeaderTextRunnable);
        headerTextView.postDelayed(resetHeaderTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    private Runnable resetHeaderTextRunnable = new Runnable() {
        @Override
        public void run() {
            headerTextView.setTextColor(
                    headerTextView.getResources().getColor(R.color.hint_color, null));
            headerTextView.setText(
                    headerTextView.getResources().getString(R.string.fingerprint_hint));
            fingerPrintIcon.setImageResource(R.drawable.ic_fp_40px);
        }
    };


    public interface FingerPrintAuthCallBack {
        /**
         * method to give a callback in case of success
         */
        void onAuthenticated();

        /**
         * method to give a callback in case of failure/Help
         */
        void onAuthenticationFailed();

    }
}
