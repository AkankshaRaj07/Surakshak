package com.surakshak.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SignUp extends AppCompatActivity {
    TextView headingText, subText, tvOtpMessage;
    LinearLayout otpBoxLayout;
    EditText editTextPhone, inputName;
    Button btnSendOtp, btnVerifyOtp;
    EditText[] otpBoxes;
    
    private FirebaseAuth mAuth;
    private String mVerificationId;
    final int OTP_BOX_COUNT = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        headingText = findViewById(R.id.headingText);
        subText = findViewById(R.id.subText);
        otpBoxLayout = findViewById(R.id.otpBoxLayout);
        editTextPhone = findViewById(R.id.editTextPhone);
        inputName = findViewById(R.id.inputName);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvOtpMessage = findViewById(R.id.tvOtpMessage);

        otpBoxLayout.setVisibility(View.GONE);
        btnVerifyOtp.setVisibility(View.GONE);
        tvOtpMessage.setVisibility(View.GONE);

        btnSendOtp.setOnClickListener(v -> {
            String mobile = editTextPhone.getText().toString().trim();
            if (mobile.length() == 10) {
                // Formatting for Firebase (requires country code, assuming +91 for India)
                sendOtp("+91" + mobile);
            } else {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show();
            }
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = getOtpFromBoxes();
            if (otp.length() == 6 && mVerificationId != null) {
                verifyOtp(otp);
            } else {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createOtpBoxes(int count) {
        otpBoxLayout.setVisibility(View.VISIBLE);
        otpBoxLayout.removeAllViews();
        otpBoxes = new EditText[count];

        for (int i = 0; i < count; i++) {
            EditText box = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 130);
            params.setMargins(8, 0, 8, 0);
            box.setLayoutParams(params);
            box.setBackgroundResource(R.drawable.box_background);
            box.setInputType(InputType.TYPE_CLASS_NUMBER);
            box.setEms(1);
            box.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            box.setTextSize(18);
            box.setTextColor(Color.BLACK);
            box.setId(View.generateViewId());

            int finalI = i;
            box.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && finalI < count - 1) {
                        otpBoxes[finalI + 1].requestFocus();
                    }
                }
            });

            otpBoxLayout.addView(box);
            otpBoxes[i] = box;
        }

        otpBoxes[0].requestFocus();
    }

    private String getOtpFromBoxes() {
        StringBuilder sb = new StringBuilder();
        if (otpBoxes != null) {
            for (EditText box : otpBoxes) {
                sb.append(box.getText().toString().trim());
            }
        }
        return sb.toString();
    }

    private void sendOtp(String mobile) {
        Toast.makeText(this, "Sending OTP via Firebase...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options =
            PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(mobile)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(SignUp.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        
                        headingText.setText("Enter OTP");
                        subText.setVisibility(View.GONE);
                        editTextPhone.setVisibility(View.GONE);
                        inputName.setVisibility(View.GONE);
                        findViewById(R.id.nameSubText).setVisibility(View.GONE);

                        createOtpBoxes(OTP_BOX_COUNT);
                        btnVerifyOtp.setVisibility(View.VISIBLE);
                        btnSendOtp.setVisibility(View.GONE);

                        tvOtpMessage.setText("An OTP has been sent via SMS.");
                        tvOtpMessage.setVisibility(View.VISIBLE);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp(String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SignUp.this, "✅ OTP Verified by Firebase!", Toast.LENGTH_SHORT).show();
                    
                    String mobile = editTextPhone.getText().toString().trim();
                    String name = inputName.getText().toString().trim();
                    String userID = task.getResult().getUser().getUid(); // Use Firebase UID

                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("userID", userID);
                    editor.putString("mobile", mobile);
                    editor.putString("name", name);
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    new AlertDialog.Builder(SignUp.this)
                            .setTitle("Set Passcode")
                            .setMessage("For your privacy and faster access, please set a secure passcode. You’ll use this passcode to unlock the app next time.\n\nYou can also enable fingerprint login after setting your passcode.")
                            .setCancelable(false)
                            .setPositiveButton("Set Passcode", (dialog, which) -> {
                                Intent intent = new Intent(SignUp.this, Passcode.class);
                                intent.putExtra("mobile", mobile);
                                intent.putExtra("name", name);
                                intent.putExtra("userID", userID);
                                startActivity(intent);
                                finish();
                            })
                            .show();
                } else {
                    if (task.getException() != null) {
                        Toast.makeText(SignUp.this, "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignUp.this, "Verification failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
}
