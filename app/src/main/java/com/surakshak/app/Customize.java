package com.surakshak.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;

public class Customize extends AppCompatActivity {

    private HashMap<String, SelectableCardView> mediumOptions = new HashMap<>();
    private SelectableCardView highRiskCard;

    private BehaviorMonitor behaviorMonitor;
    private String userID;
    private final Handler behaviorHandler = new Handler();
    private final Runnable sendBehaviorRunnable = new Runnable() {
        @Override
        public void run() {
            if (behaviorMonitor != null) {
                JSONObject behaviorVector = behaviorMonitor.getBehaviorVectorAsJSON(userID);
                if (behaviorVector != null) {
                    Log.d("Customize", "Behavior Vector: " + behaviorVector.toString());
                    // You can send this to backend using Volley if needed
                }
            }
            behaviorHandler.postDelayed(this, 20000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userID = prefs.getString("userID", null);
        if (userID == null) {
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        behaviorMonitor = new BehaviorMonitor(this);
        behaviorMonitor.startMonitoring();
        behaviorMonitor.trackTouch(findViewById(android.R.id.content));
        behaviorHandler.post(sendBehaviorRunnable);

        GridLayout mediumGrid = findViewById(R.id.mediumGrid);
        LinearLayout highRiskContainer = findViewById(R.id.highRiskContainer);
        SharedPreferences riskPrefs = getSharedPreferences("RiskPrefs", MODE_PRIVATE);

        // Add medium cards
        addMediumCard(mediumGrid, "Ask Passcode", R.drawable.ic_lock, "medium_passcode", riskPrefs);
        addMediumCard(mediumGrid, "Ask Biometric", R.drawable.ic_fingerprint, "medium_biometric", riskPrefs);
        addMediumCard(mediumGrid, "Ask Typing Phrase", R.drawable.ic_keyboard, "medium_phrase", riskPrefs);
        addMediumCard(mediumGrid, "Ask Gesture", R.drawable.ic_gesture, "medium_gesture", riskPrefs);

        // Add high risk card (single selectable)
        highRiskCard = new SelectableCardView(this, true);
        highRiskCard.setTitle("Force Logout");
        highRiskCard.setIcon(R.drawable.ic_logout2);
        highRiskCard.setChecked(true);
        highRiskContainer.addView(highRiskCard);
    }

    private void addMediumCard(GridLayout grid, String label, int iconRes, String key, SharedPreferences prefs) {
        SelectableCardView card = new SelectableCardView(this, false);
        card.setTitle(label);
        card.setIcon(iconRes);
        card.setChecked(prefs.getBoolean(key, false));
        grid.addView(card);
        mediumOptions.put(key, card);
    }

    public void onSaveClick(View view) {
        if (!highRiskCard.isChecked()) {
            Toast.makeText(this, "High-risk option must be selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("RiskPrefs", MODE_PRIVATE).edit();

        for (String key : mediumOptions.keySet()) {
            editor.putBoolean(key, mediumOptions.get(key).isChecked());
        }

        editor.putBoolean("high_logout", highRiskCard.isChecked());
        editor.apply();

        Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onBackClick(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (behaviorMonitor != null) {
            behaviorMonitor.stopMonitoring();
        }
        behaviorHandler.removeCallbacks(sendBehaviorRunnable);
    }

    class SelectableCardView extends FrameLayout {

        private LinearLayout container;
        private ImageView iconView;
        private TextView titleView;
        private ImageView tickView;
        private boolean isChecked = false;
        private boolean singleSelect;

        public SelectableCardView(Context context, boolean singleSelect) {
            super(context);
            this.singleSelect = singleSelect;

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E3F2FD"));
            bg.setCornerRadius(24);
            bg.setStroke(2, Color.parseColor("#90CAF9"));
            setBackground(bg);

            setPadding(20, 20, 20, 20);
            setClickable(true);
            setFocusable(true);

            container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(layoutParams);

            iconView = new ImageView(context);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(100, 100);
            iconView.setLayoutParams(iconParams);
            iconView.setColorFilter(Color.parseColor("#0D47A1"));
            container.addView(iconView);

            titleView = new TextView(context);
            titleView.setTextColor(Color.BLACK);
            titleView.setTextSize(16);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(0, 12, 0, 0);
            container.addView(titleView);

            addView(container);

            tickView = new ImageView(context);
            tickView.setImageResource(R.drawable.ic_check);
            tickView.setColorFilter(Color.parseColor("#0D47A1"));
            tickView.setVisibility(View.GONE);

            LayoutParams tickParams = new LayoutParams(48, 48);
            tickParams.gravity = Gravity.END | Gravity.TOP;
            tickParams.setMargins(0, 0, 8, 0);
            tickView.setLayoutParams(tickParams);

            addView(tickView);

            setOnClickListener(v -> {
                if (singleSelect) {
                    setChecked(true);
                } else {
                    setChecked(!isChecked);
                }
            });

            int size = getResources().getDisplayMetrics().widthPixels / 2 - 130;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = 240;
            params.setMargins(35, 30, 35, 30);
            setLayoutParams(params);
        }

        public void setIcon(int resId) {
            iconView.setImageResource(resId);
        }

        public void setTitle(String title) {
            titleView.setText(title);
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
            tickView.setVisibility(checked ? View.VISIBLE : View.GONE);
        }

        public boolean isChecked() {
            return isChecked;
        }
    }
}
