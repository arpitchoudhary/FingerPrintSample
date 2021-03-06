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
import java.io.UnsupportedEncodingException;
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
import javax.crypto.spec.IvParameterSpec;

public class MainActivity extends AppCompatActivity implements LoginView {

    private static final String TOUCH_ID_OPTED = "TOUCH_ID_OPTED";
    public static final String CHARSET_NAME = "UTF-8";
    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private KeyStore keyStore;
    private final String KEY_NAME = "Finger_key";
    public static final String EXTRA_KEY_TOUCH_ID = "EXTRA_KEY_TOUCH_ID";

    private Button loginButton;
    private TextView screenText;
    private SwitchCompat touchIdToggle;
    private EditText userName, password;
    private MockLoginHelper mockLoginHelper;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private String pwd;
    private SecretKey secretKey;

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
                    secretKey = generateKey();
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
                    dialogHelper.setCryptoObject(null);
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
    public SecretKey generateKey() throws FingerPrintException {

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
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();

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
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
            if (cipherMode == Cipher.ENCRYPT_MODE) {
                cipher.init(cipherMode, secretKey);
                byte[] encryptionIv = cipher.getIV();
                editor = prefs.edit();
                editor.putString("encryptionIv", Base64.encodeToString(encryptionIv, Base64.DEFAULT));
                editor.apply();
            } else {
                String base64EncryptionIv = prefs.getString("encryptionIv", null);
                byte[] encryptionIv = Base64.decode(base64EncryptionIv, Base64.DEFAULT);
                cipher.init(cipherMode, secretKey, new IvParameterSpec(encryptionIv));
            }
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | UnrecoverableKeyException | KeyStoreException | InvalidAlgorithmParameterException e) {
            throw new FingerPrintException("Exception in generating Cipher");
        }
    }

    public void beginSignInWithFingerTouch(boolean authentication) {

        if (authentication) {
            beginLoginProcess();
        } else {
            mockLoginHelper.presentFailureView();
        }

    }


    @Override
    public void updateSuccessfulLoginSetup() {
        if (touchIdToggle.isChecked()) {
            saveCredential();
        }

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra(EXTRA_KEY_TOUCH_ID, touchIdToggle.isChecked());
        startActivity(intent);

    }

    private void saveCredential() {
        try {

            Cipher cipher = generateCipher(Cipher.ENCRYPT_MODE);
            byte[] passwordBytes = pwd.getBytes(CHARSET_NAME);
            byte[] encryptedPasswordBytes = cipher.doFinal(passwordBytes);
            String encryptedPassword = Base64.encodeToString(encryptedPasswordBytes, Base64.DEFAULT);

            editor = prefs.edit();
            editor.putBoolean(TOUCH_ID_OPTED, true);
            editor.putString("password", encryptedPassword);
            editor.apply();

        } catch (FingerPrintException | BadPaddingException | UnsupportedEncodingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loginFail() {
        screenText.setText("Authentication Failure!!!!");
    }

    public void beginLoginProcess() {
        try {
            Cipher cipher = generateCipher(Cipher.DECRYPT_MODE);
            String base64EncryptedPassword = prefs.getString("password", null);
            byte[] encryptedPassword = Base64.decode(base64EncryptedPassword, Base64.DEFAULT);
            byte[] passwordBytes = cipher.doFinal(encryptedPassword);
            String password = new String(passwordBytes, CHARSET_NAME);
            Toast.makeText(this, password + " got ", Toast.LENGTH_SHORT).show();
            mockLoginHelper.callLoginService("UserName", password);
        } catch (FingerPrintException | BadPaddingException | UnsupportedEncodingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
