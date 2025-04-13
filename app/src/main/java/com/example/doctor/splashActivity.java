package com.example.doctor;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class splashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Delay 2 seconds, then go to LoginActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(splashActivity.this, LoginActivity.class));
            finish();
        }, 2000);
    }
}
