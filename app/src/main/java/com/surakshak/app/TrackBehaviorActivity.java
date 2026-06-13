package com.surakshak.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.surakshak.app.utils.Constants;  // Import Constants for future use

public class TrackBehaviorActivity extends AppCompatActivity {

    private Switch trackingSwitch;
    private TextView trackingStatus, behaviorLog;
    private BehaviorMonitor behaviorMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        trackingSwitch = findViewById(R.id.switchTracking);
        trackingStatus = findViewById(R.id.txtTrackingStatus);
        behaviorLog = findViewById(R.id.txtBehaviorLog);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isTrackingEnabled = prefs.getBoolean("behavior_tracking_enabled", true);
        trackingSwitch.setChecked(isTrackingEnabled);
        updateStatusText(isTrackingEnabled);

        behaviorMonitor = new BehaviorMonitor(this);
        behaviorMonitor.trackTouch(findViewById(android.R.id.content));

        if (isTrackingEnabled) {
            behaviorMonitor.startMonitoring();
        }

        trackingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("behavior_tracking_enabled", isChecked);
            editor.apply();
            updateStatusText(isChecked);

            if (isChecked) {
                behaviorMonitor.startMonitoring();
            } else {
                behaviorMonitor.stopMonitoring();
            }

            Toast.makeText(this, isChecked ? "Behavior Tracking Enabled" : "Behavior Tracking Disabled", Toast.LENGTH_SHORT).show();
        });

        showDummyBehaviorLog();
    }

    private void updateStatusText(boolean isEnabled) {
        trackingStatus.setText(isEnabled ? "Behavior Tracking is ON" : "Behavior Tracking is OFF");
    }

    private void showDummyBehaviorLog() {
        String log = "Behavior Metrics:\n\n"
                + "•   Typing Speed : 230 ms/keystroke\n\n"
                + "•   Dwell Time : 100\n\n"
                + "•   Flight Time : 120\n\n"
                + "•   InterKey Delay : 150\n\n"
                + "•   Tap Pressure : 0.8\n\n"
                + "•   Screen Hold Time : 1.2s average\n\n"
                + "•   Tilt Angle : Normal\n\n"
                + "•   Touch Size : 0.02";
        behaviorLog.setText(log);
    }

    public void onBackClick(View view) {
        finish();
    }

    public void onClearClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Tracked Data");
        builder.setMessage("You are about to delete your tracked behavioural data. Then you have to again go through the Training session period.");
        builder.setCancelable(false);

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("OK", (dialog, which) -> {
            Toast.makeText(this, "Tracked behavioral data cleared.", Toast.LENGTH_SHORT).show();
            // Clear logic can be placed here if needed
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (behaviorMonitor != null) {
            behaviorMonitor.stopMonitoring();
        }
    }
}
