package com.surakshak.app;

import android.app.Dialog;
import android.content.Intent;
import android.app.AlertDialog;
import android.os.Handler;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.view.Gravity;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;

import static com.surakshak.app.utils.Constants.USER_API;

public class Login extends AppCompatActivity {

    LinearLayout passcodeLayout;
    EditText[] passcodeBoxes;
    Button btnLogin;
    TextView greetingText, tvForgot;

    final int BOX_COUNT = 6;
    String mobile, userID;
    int resetAttempts = 0;
    final int MAX_RESETS = 3;

    private FirebaseAuth mAuth;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        passcodeLayout = findViewById(R.id.passcodeBoxLayout);
        btnLogin = findViewById(R.id.btnLogin);
        greetingText = findViewById(R.id.greetingText);
        tvForgot = findViewById(R.id.tvForgotPasscode);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        mobile = prefs.getString("mobile", null);
        userID = prefs.getString("userID", null);

        if (mobile == null || userID == null) {
            startActivity(new Intent(Login.this, SignUp.class));
            finish();
            return;
        }

        fetchUserNameAndShowGreeting(mobile);
        createBoxes(passcodeLayout, BOX_COUNT);

        btnLogin.setOnClickListener(v -> {
            String enteredPass = getBoxValue(passcodeBoxes);

            if (enteredPass.length() == BOX_COUNT) {
                SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                int selectedGesture = sp.getInt("selectedGesture", 0);

                if (selectedGesture == 1 && PanicGesture1.isLoginPanicPin(enteredPass, mobile)) {
                    goToFakeHomeWithServerDownDialog();
                } else if (selectedGesture == 2 && PanicGesture2.isLoginPanicPin(enteredPass, mobile)) {
                    goToFakeHomeWithServerDownDialog();
                } else {
                    verifyPasscode(userID, enteredPass);
                }

            } else {
                Toast.makeText(this, "Enter 6-digit passcode", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgot.setOnClickListener(v -> {
            if (resetAttempts >= MAX_RESETS) {
                Toast.makeText(this, "You have reached max reset attempts today.", Toast.LENGTH_LONG).show();
            } else {
                showForgotPasscodeDialog();
            }
        });

        biometricLogin();
    }

    private void biometricLogin() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                runOnUiThread(() -> {
                    Toast.makeText(Login.this, "Biometric authentication successful", Toast.LENGTH_SHORT).show();
                    goToHome();
                });
            }

            @Override
            public void onAuthenticationFailed() {
                runOnUiThread(() -> Toast.makeText(Login.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show());
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login with Biometric")
                .setSubtitle("Use fingerprint or face to login")
                .setNegativeButtonText("Use Passcode")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void fetchUserNameAndShowGreeting(String mobile) {
        try {
            JSONObject params = new JSONObject().put("mobile", mobile);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    USER_API + "/getName",
                    params,
                    response -> {
                        String name = response.optString("name", "User");
                        if (name.equals("") || name.equals("null")) name = "User";
                        greetingText.setText(getGreeting() + ", " + name);
                    },
                    error -> greetingText.setText(getGreeting() + ", User")
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
            greetingText.setText(getGreeting() + ", User");
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        else if (hour < 17) return "Good Afternoon";
        else return "Good Evening";
    }

    private void verifyPasscode(String userID, String passcode) {
        try {
            JSONObject params = new JSONObject().put("userID", userID).put("passcode", passcode);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    USER_API + "/login",
                    params,
                    response -> {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    },
                    error -> Toast.makeText(this, "Invalid passcode", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToHome() {
        startActivity(new Intent(Login.this, Homescreen.class));
        finish();
    }

    private void goToFakeHomeWithServerDownDialog() {
        PanicUtils.triggerPanicLock(this);
        Intent i = new Intent(Login.this, FakeHomescreen.class);
        new Handler().postDelayed(this::showServerDownDialog, 1000);
    }

    private void showServerDownDialog() {
        new AlertDialog.Builder(Login.this)
                .setTitle("Banking Server Down")
                .setMessage("Please try again later.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finishAffinity())
                .show();
    }

    private void createBoxes(LinearLayout layout, int count) {
        layout.removeAllViews();
        passcodeBoxes = new EditText[count];

        for (int i = 0; i < count; i++) {
            EditText box = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 130);
            params.setMargins(8, 0, 8, 0);
            box.setLayoutParams(params);
            box.setBackgroundResource(R.drawable.box_background);
            box.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            box.setTextColor(Color.BLACK);
            box.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            box.setTextSize(18);
            box.setId(View.generateViewId());

            int finalI = i;
            box.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && finalI < count - 1)
                        passcodeBoxes[finalI + 1].requestFocus();
                }
            });

            layout.addView(box);
            passcodeBoxes[i] = box;
        }
        passcodeBoxes[0].requestFocus();
    }

    private String getBoxValue(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText box : boxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }

    private EditText[] createInlineBoxes(Dialog dialog, int containerId) {
        LinearLayout layout = dialog.findViewById(containerId);
        layout.removeAllViews();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);

        EditText[] boxes = new EditText[6];

        for (int i = 0; i < 6; i++) {
            EditText b = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 140);
            params.setMargins(8, 8, 8, 8);
            b.setLayoutParams(params);
            b.setInputType(InputType.TYPE_CLASS_NUMBER);
            b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(R.drawable.box_background);
            b.setTextSize(18);
            b.setTextColor(Color.BLACK);

            final int index = i;
            b.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < 5) {
                        boxes[index + 1].requestFocus();
                    }
                }
            });

            layout.addView(b);
            boxes[i] = b;
        }

        boxes[0].requestFocus();
        return boxes;
    }

    private void sendOtpForReset(String mobile) {
        Toast.makeText(this, "Sending OTP via Firebase...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options =
            PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber("+91" + mobile) // Assuming +91 India code for Firebase formatting
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Automatically handled
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(Login.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        Toast.makeText(Login.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showForgotPasscodeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_forgot_passcode);
        dialog.setCancelable(false);

        LinearLayout step1Layout = dialog.findViewById(R.id.step1Layout);
        LinearLayout step2Layout = dialog.findViewById(R.id.step2Layout);

        Button btnProceed = dialog.findViewById(R.id.btnProceed);
        Button btnVerifyOtp = dialog.findViewById(R.id.btnVerifyOtp);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        EditText[] otpBoxes = createInlineBoxes(dialog, R.id.otpLayout);

        step1Layout.setVisibility(View.VISIBLE);
        step2Layout.setVisibility(View.GONE);

        btnProceed.setOnClickListener(v -> {
            step1Layout.setVisibility(View.GONE);
            step2Layout.setVisibility(View.VISIBLE);
            sendOtpForReset(mobile);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = getBoxValue(otpBoxes);
            if (otp.length() != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpAndOpenPasscodeDialog(otp, dialog);
        });

        dialog.show();
    }

    private void verifyOtpAndOpenPasscodeDialog(String otp, Dialog parentDialog) {
        if (mVerificationId == null) {
            Toast.makeText(this, "Wait for OTP to be sent", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "OTP Verified", Toast.LENGTH_SHORT).show();
                    parentDialog.dismiss();
                    showPasscodeResetDialog();
                } else {
                    resetAttempts++;
                    Toast.makeText(this, "Invalid OTP: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updatePasscodeOnServer(String newPass, Dialog dialog) {
        try {
            JSONObject params = new JSONObject()
                    .put("mobile", mobile)
                    .put("passcode", newPass);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    USER_API + "/register",
                    params,
                    response -> {
                        Toast.makeText(this, "Passcode reset! Please login.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    },
                    error -> Toast.makeText(this, "Failed to update passcode", Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPasscodeResetDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_reset_passcode);
        dialog.setCancelable(false);

        LinearLayout newPasscodeLayout = dialog.findViewById(R.id.newPasscodeLayout);
        LinearLayout confirmPasscodeLayout = dialog.findViewById(R.id.confirmPasscodeLayout);
        Button btnResetPasscode = dialog.findViewById(R.id.btnResetPasscode);

        EditText[] newPasscodeBoxes = createInlineBoxes(dialog, R.id.newPasscodeLayout);
        EditText[] confirmPasscodeBoxes = createInlineBoxes(dialog, R.id.confirmPasscodeLayout);

        btnResetPasscode.setOnClickListener(v -> {
            String p1 = getBoxValue(newPasscodeBoxes);
            String p2 = getBoxValue(confirmPasscodeBoxes);

            if (p1.length() != 6 || p2.length() != 6) {
                Toast.makeText(this, "Passcodes must be 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p1.equals(p2)) {
                Toast.makeText(this, "Passcodes do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePasscodeOnServer(p1, dialog);
        });

        dialog.show();
    }
}
