package com.arpit.fingerprintsam;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity implements LoginView {

    private static final String TOUCH_ID_OPTED = "TOUCH_ID_OPTED";
    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private KeyStore keyStore;
    private FingerprintManager.CryptoObject cryptoObject;
    private final String KEY_NAME = "Finger_key";
    private static final String SECRET_MESSAGE = "Very secret message";
    public static final String EXTRA_KEY_TOUCH_ID = "EXTRA_KEY_TOUCH_ID";

    private Button loginButton;
    private TextView screenText;
    private SwitchCompat touchIdToggle;
    private EditText userName, password;
    private MockLoginHelper mockLoginHelper;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private String pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = (Button) findViewById(R.id.button1);
        screenText = (TextView) findViewById(R.id.txtViewForMessage);
        touchIdToggle = (SwitchCompat) findViewById(R.id.touchIdSwitch);
        userName = (EditText) findViewById(R.id.editTextName);
        password = (EditText) findViewById(R.id.editTextPassword);

        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        mockLoginHelper = new MockLoginHelper();
        mockLoginHelper.setView(this);

        prefs = getSharedPreferences("SYSTEMPREF", MODE_PRIVATE);
        editor = prefs.edit();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkForFingerPrintSupport()) {
                    loginButton.setEnabled(false);
                } else {
                    generateKey();
                    Cipher cipher = generateCipher(Cipher.ENCRYPT_MODE);
                    cryptoObject = new FingerprintManager.CryptoObject(cipher);
                }
            }
        } catch (FingerPrintException e) {
            loginButton.setEnabled(false);
        }


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (prefs.getBoolean(TOUCH_ID_OPTED, false)) {
                    // launch the dialog fragment for the touch id
                    FingerPrintDialogHelper dialogHelper = new FingerPrintDialogHelper();
                    dialogHelper.setFingerPrintManager(fingerprintManager);
                    dialogHelper.setCryptoObject(cryptoObject);
                    dialogHelper.show(getSupportFragmentManager(), "FingerPrintDialogHelper");
                } else {
                    getLoginDetailsFromScreen();
                }
            }
        });

    }

    private void getLoginDetailsFromScreen() {

        String user = userName.getText().toString();
        pwd = password.getText().toString();

        if (user.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "Enter UserName && Pass", Toast.LENGTH_SHORT).show();
            return;
        }

        // assuming everything is good with validation, lets create a helper class to mock the actual service call.
        mockLoginHelper.callLoginService(user, pwd);

    }

    /**
     * method to check for the support for the fingerPrint
     *
     * @return
     * @throws FingerPrintException
     */
    private boolean checkForFingerPrintSupport() throws FingerPrintException {

        try {
            // Lock Screen Security not enabled in settings
            if (!keyguardManager.isKeyguardSecure()) {
                return false;
            }

            // Check if the fingerprint sensor is present
            if (!fingerprintManager.isHardwareDetected()) {
                screenText.setText("Hardware Not supported For FingerPrint");
                return false;
            }

            // check for the if there is any fingerprint Enrolled
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                screenText.setText("Register at least one fingerprint");
                return false;
            }

        } catch (SecurityException e) {
            throw new FingerPrintException("Exception in checking FingerPrint support");
        }

        return true;
    }

    /**
     * Now we need to have a method which get the access to android keystore and generate the key
     * this key will then be used for encryption and decryption
     */
    public void generateKey() throws FingerPrintException {

        KeyGenerator keyGenerator = null;
        try {
            // getting the reference to the keyStore
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            // key generator to generate the key
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();

        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException |
                CertificateException | IOException | InvalidAlgorithmParameterException e) {
            throw new FingerPrintException("Exception in generating Key");
        }
    }


    /**
     * method to generate the cipher which will be using the generated key.
     *
     * @return
     */
    private Cipher generateCipher(int cipherMode) throws FingerPrintException {

        try {
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);
            cipher.init(cipherMode, key);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | UnrecoverableKeyException | KeyStoreException | InvalidKeyException | CertificateException | IOException e) {
            throw new FingerPrintException("Exception in generating Cipher");
        }

    }

    public void beginEncryption(boolean isEncryptionEnabled) {
        if (isEncryptionEnabled) {
            try {
                byte[] encrypted = cryptoObject.getCipher().doFinal(pwd.getBytes());
                screenText.setText(Base64.encodeToString(encrypted, 0 /* flags */));
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                screenText.setText("encryption failed!!!");
            }
        } else {
            screenText.setText("AuthenticationFailed");
        }
    }


    @Override
    public void updateSuccessfulLoginSetup() {
        if (touchIdToggle.isChecked()) {
            editor = prefs.edit();
            editor.putBoolean(TOUCH_ID_OPTED, true);
            editor.apply();
        }

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra(EXTRA_KEY_TOUCH_ID, touchIdToggle.isChecked());
        startActivity(intent);

    }

    @Override
    public void loginFail() {

    }
}
