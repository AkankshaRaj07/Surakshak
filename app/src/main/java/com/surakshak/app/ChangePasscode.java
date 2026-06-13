package com.surakshak.app;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.BASE_IP;

public class ChangePasscode extends AppCompatActivity {

    String mobile, userID;
    String userUrl = BASE_IP + ":5000/api/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        mobile = prefs.getString("mobile", null);
        userID = prefs.getString("userID", null);

        if (mobile == null || userID == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showChangePasscodeDialog();
    }

    private void showChangePasscodeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_passcode);
        dialog.setCancelable(false);

        LinearLayout step1Layout = dialog.findViewById(R.id.step1Layout);
        LinearLayout step2Layout = dialog.findViewById(R.id.step2Layout);

        Button btnProceed = dialog.findViewById(R.id.btnProceed);
        Button btnVerifyOtp = dialog.findViewById(R.id.btnVerifyOtp);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        EditText[] otpBoxes = createInlineBoxes(dialog, R.id.otpLayout);

        step1Layout.setVisibility(View.VISIBLE);
        step2Layout.setVisibility(View.GONE);

        btnProceed.setOnClickListener(v -> {
            step1Layout.setVisibility(View.GONE);
            step2Layout.setVisibility(View.VISIBLE);
            sendOtp();
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = getBoxValue(otpBoxes);
            if (otp.length() != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpAndContinue(otp, dialog);
        });

        dialog.show();
    }

    private void sendOtp() {
        try {
            JSONObject params = new JSONObject().put("mobile", mobile);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_IP + ":5000/api/otp/send",
                    params,
                    resp -> Toast.makeText(this, "OTP sent", Toast.LENGTH_SHORT).show(),
                    err -> Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyOtpAndContinue(String otp, Dialog parentDialog) {
        try {
            JSONObject params = new JSONObject().put("mobile", mobile).put("otp", otp);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_IP + ":5000/api/otp/verify",
                    params,
                    response -> {
                        Toast.makeText(this, "OTP Verified", Toast.LENGTH_SHORT).show();
                        parentDialog.dismiss();
                        showPasscodeResetDialog();
                    },
                    error -> Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
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

        EditText[] newPasscodeBoxes = createInlineBoxes(dialog, R.id.newPasscodeLayout);
        EditText[] confirmPasscodeBoxes = createInlineBoxes(dialog, R.id.confirmPasscodeLayout);
        Button btnResetPasscode = dialog.findViewById(R.id.btnResetPasscode);

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

            updatePasscode(p1, dialog);
        });

        dialog.show();
    }

    private void updatePasscode(String newPass, Dialog dialog) {
        try {
            JSONObject params = new JSONObject()
                    .put("mobile", mobile)
                    .put("passcode", newPass)
                    .put("userID", userID);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    userUrl + "/register",
                    params,
                    response -> {
                        Toast.makeText(this, "Passcode changed successfully!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        finish();
                    },
                    error -> Toast.makeText(this, "Failed to update passcode", Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            b.setTextColor(android.graphics.Color.BLACK);

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

    private String getBoxValue(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText box : boxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }
}
