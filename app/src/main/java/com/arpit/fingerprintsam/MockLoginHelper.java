package com.arpit.fingerprintsam;

/**
 * Created by arpit on 4/11/17.
 */

public class MockLoginHelper {

    private LoginView view;

    public MockLoginHelper() {

    }

    public void setView(LoginView view) {
        this.view = view;
    }

    public void callLoginService(String user, String pwd) {
        try {
            Thread.sleep(200);
            view.updateSuccessfulLoginSetup();
        } catch (InterruptedException e) {
            view.loginFail();
        }

    }

    public void presentFailureView() {
        view.loginFail();
    }
}
