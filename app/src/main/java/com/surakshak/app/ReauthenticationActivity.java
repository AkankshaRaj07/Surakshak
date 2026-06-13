package com.surakshak.app;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.concurrent.Executor;
//This screen is connected with the help button for now but is to be triggered according to the risk score
public class ReauthenticationActivity extends AppCompatActivity {

    private LinearLayout bottomContainer;
    private SharedPreferences prefs;
    private Context context;
    private boolean isLocked = false;
    private String userUrl = "http://192.168.1.4:5000/api/user"; // backend URL
    private String mobile; // User's mobile number from prefs or Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reauthentication);

        context = this;
        prefs = getSharedPreferences("RiskPrefs", MODE_PRIVATE);
        bottomContainer = findViewById(R.id.bottomContainer);

        mobile = getSharedPreferences("SurakshaPrefs", MODE_PRIVATE).getString("mobile", ""); // get mobile number

        String riskLevel = getIntent().getStringExtra("risk_level");

        // SIMULATION ONLY: If you want to test, override manually
        // riskLevel = "medium"; // uncomment to test medium risk

        if (riskLevel == null || riskLevel.equals("low")) {
            finish(); // let user continue
        } else if (riskLevel.equals("medium")) {
            showMediumRiskUI();
        } else if (riskLevel.equals("high")) {
            showHighRiskDialog();
        }
    }

    private void showMediumRiskUI() {
        bottomContainer.setVisibility(View.VISIBLE);
        bottomContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));

        TextView title = findViewById(R.id.riskTitle);
        title.setText("Medium Risk Detected");
        title.setTextColor(Color.parseColor("#FFC107")); // Yellow

        ImageView icon = findViewById(R.id.riskIcon);
        icon.setImageResource(R.drawable.ic_warning); // warning icon

        LinearLayout optionBox = findViewById(R.id.reauthOptionsBox);
        optionBox.removeAllViews();

        if (prefs.getBoolean("medium_passcode", false)) {
            optionBox.addView(createOptionButton("Ask Passcode", v -> handlePasscode()));
        }
        if (prefs.getBoolean("medium_biometric", false)) {
            optionBox.addView(createOptionButton("Ask Biometric", v -> handleBiometric()));
        }
        if (prefs.getBoolean("medium_phrase", false)) {
            optionBox.addView(createOptionButton("Typing Phrase", v -> {
                Toast.makeText(context, "Typing phrase not implemented.", Toast.LENGTH_SHORT).show();
            }));
        }
        if (prefs.getBoolean("medium_gesture", false)) {
            optionBox.addView(createOptionButton("Gesture", v -> {
                Toast.makeText(context, "Gesture not implemented.", Toast.LENGTH_SHORT).show();
            }));
        }
    }

    private Button createOptionButton(String text, View.OnClickListener listener) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.BLACK);
        btn.setBackgroundResource(R.drawable.reauth_box);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        btn.setLayoutParams(params);
        return btn;
    }

    private void handlePasscode() {
        bottomContainer.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Passcode");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);

        EditText[] passcodeBoxes = new EditText[6];

        for (int i = 0; i < 6; i++) {
            EditText box = new EditText(context);
            box.setInputType(InputType.TYPE_CLASS_NUMBER);
            box.setTextColor(Color.BLACK);
            box.setGravity(Gravity.CENTER);
            box.setMaxEms(1);
            box.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            box.setBackgroundResource(R.drawable.box_background);

            // Size box manually
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100); // size in px
            params.setMargins(8, 8, 8, 8);
            box.setLayoutParams(params);

            final int index = i;
            passcodeBoxes[i] = box;

            box.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < 5) {
                        passcodeBoxes[index + 1].requestFocus();
                    }
                }
            });

            box.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (box.getText().toString().isEmpty() && index > 0) {
                        passcodeBoxes[index - 1].requestFocus();
                    }
                }
                return false;
            });

            layout.addView(box);
        }

        builder.setView(layout);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            StringBuilder entered = new StringBuilder();
            for (EditText d : passcodeBoxes) {
                String digit = d.getText().toString().trim();
                if (digit.isEmpty()) {
                    Toast.makeText(context, "Enter all 6 digits", Toast.LENGTH_SHORT).show();
                    return;
                }
                entered.append(digit);
            }
            verifyPasscode(mobile, entered.toString());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> showMediumRiskUI());
        builder.show();
    }

    private void verifyPasscode(String mobile, String passcode) {
        try {
            JSONObject params = new JSONObject().put("mobile", mobile).put("passcode", passcode);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    userUrl + "/login", // Same login endpoint
                    params,
                    response -> showSuccessMessage(),
                    error -> {
                        Toast.makeText(this, "Invalid passcode", Toast.LENGTH_SHORT).show();
                        showMediumRiskUI();
                    }
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBiometric() {
        bottomContainer.setVisibility(View.GONE);

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        showSuccessMessage();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(context, "Biometric Failed", Toast.LENGTH_SHORT).show();
                        showMediumRiskUI();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        Toast.makeText(context, "Biometric Error", Toast.LENGTH_SHORT).show();
                        showMediumRiskUI();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Reauthenticate with Biometric")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showSuccessMessage() {
        new AlertDialog.Builder(context)
                .setTitle("Reauthenticated")
                .setMessage("Successfully reauthenticated. You may continue.")
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    private void showHighRiskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("High Risk Detected");
        builder.setIcon(R.drawable.ic_danger); // red danger icon
        builder.setMessage("To ensure your security, we are forcibly logging you out.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (d, w) -> logoutAndLockApp());
        builder.show();
    }
    private void logoutAndLockApp() {
        if (isLocked) return;

        isLocked = true;

        // Save lock end time to prefs (current + 5 mins)
        long unlockTime = System.currentTimeMillis() + 5 * 60 * 1000;
        getSharedPreferences("SurakshaPrefs", MODE_PRIVATE)
                .edit()
                .putLong("lock_until", unlockTime)
                .apply();

        Toast.makeText(context, "You are logged out. App locked for 5 minutes.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}
