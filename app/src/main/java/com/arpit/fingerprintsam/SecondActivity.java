package com.arpit.fingerprintsam;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by arpit on 4/11/17.
 */

public class SecondActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_acitivity);
        final TextView txt = (TextView) findViewById(R.id.textSecond);

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_KEY_TOUCH_ID, false)) {
            txt.setText("FingerPrint Enabled");
        } else {
            txt.setText("FingerPrint Not Enabled");
        }

    }
}
