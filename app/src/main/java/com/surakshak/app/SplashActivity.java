package com.surakshak.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    TextView appName;
    String fullText = "Surakshak";
    int index = 0;
    long delay = 150;
    Handler handler = new Handler();

    Runnable typeRunnable = new Runnable() {
        @Override
        public void run() {
            if (index <= fullText.length()) {
                appName.setText(fullText.substring(0, index));
                index++;
                handler.postDelayed(this, delay);
            } else {
                handler.postDelayed(() -> {
                    // ✅ FIX: Use correct prefs
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
                    String mobile = prefs.getString("mobile", null);
                    String userID = prefs.getString("userID", null); // Correct source

                    if (isLoggedIn && mobile != null && userID != null) {
                        startActivity(new Intent(SplashActivity.this, Login.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, SignUp.class));
                    }

                    overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
                    finish();
                }, 1500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        appName = findViewById(R.id.appName);
        handler.postDelayed(typeRunnable, delay);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);  // ✅ Changed here too
        long lockUntil = prefs.getLong("panicLockTimestamp", 0);

        if (System.currentTimeMillis() < lockUntil) {
            Toast.makeText(this, "App temporarily locked", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
