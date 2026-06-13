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
import static com.surakshak.app.utils.Constants.USER_API;

public class TPINActivity extends AppCompatActivity {

    String mobile, userID;
    String userUrl = USER_API;
    String otpUrl = BASE_IP + ":5000/api/otp";

    private BehaviorMonitor behaviorMonitor;

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

        behaviorMonitor = new BehaviorMonitor(this);
        behaviorMonitor.startMonitoring();

        showSetTPINDialog();
    }

    private void showSetTPINDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_set_tpin);
        dialog.setCancelable(false);

        LinearLayout step1 = dialog.findViewById(R.id.step1Layout);
        LinearLayout step2 = dialog.findViewById(R.id.step2Layout);
        LinearLayout step3 = dialog.findViewById(R.id.step3Layout);

        Button btnProceed = dialog.findViewById(R.id.btnProceed);
        Button btnVerifyOtp = dialog.findViewById(R.id.btnVerifyOtp);
        Button btnSetTPIN = dialog.findViewById(R.id.btnSetTPIN);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        EditText[] otpBoxes = createBoxes(dialog, R.id.otpLayout, 6);
        EditText[] newTPIN = createBoxes(dialog, R.id.newTPINLayout, 4);
        EditText[] confirmTPIN = createBoxes(dialog, R.id.confirmTPINLayout, 4);

        for (EditText box : otpBoxes) behaviorMonitor.attachToEditText(box);
        for (EditText box : newTPIN) behaviorMonitor.attachToEditText(box);
        for (EditText box : confirmTPIN) behaviorMonitor.attachToEditText(box);
        behaviorMonitor.trackTouch(dialog.findViewById(android.R.id.content));

        step1.setVisibility(View.VISIBLE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);

        btnProceed.setOnClickListener(v -> {
            step1.setVisibility(View.GONE);
            step2.setVisibility(View.VISIBLE);
            sendOtp();
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = getBoxValue(otpBoxes);
            if (otp.length() != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpWithServer(otp, step2, step3);
        });

        btnSetTPIN.setOnClickListener(v -> {
            String tp1 = getBoxValue(newTPIN);
            String tp2 = getBoxValue(confirmTPIN);
            if (tp1.length() != 4 || tp2.length() != 4) {
                Toast.makeText(this, "Enter 4-digit TPIN", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!tp1.equals(tp2)) {
                Toast.makeText(this, "TPINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            updateTPIN(tp1, dialog); // Send plain TPIN
        });

        dialog.show();
    }

    private void sendOtp() {
        try {
            JSONObject params = new JSONObject().put("mobile", mobile);
            JsonObjectRequest rq = new JsonObjectRequest(
                    Request.Method.POST,
                    otpUrl + "/send",
                    params,
                    resp -> Toast.makeText(this, "OTP sent", Toast.LENGTH_SHORT).show(),
                    err -> Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(rq);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyOtpWithServer(String otp, View step2, View step3) {
        try {
            JSONObject params = new JSONObject()
                    .put("mobile", mobile)
                    .put("otp", otp);
            JsonObjectRequest rq = new JsonObjectRequest(
                    Request.Method.POST,
                    otpUrl + "/verify",
                    params,
                    resp -> {
                        Toast.makeText(this, "OTP verified", Toast.LENGTH_SHORT).show();
                        step2.setVisibility(View.GONE);
                        step3.setVisibility(View.VISIBLE);
                    },
                    err -> Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(rq);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTPIN(String tpin, Dialog dialog) {
        try {
            JSONObject params = new JSONObject()
                    .put("mobile", mobile)
                    .put("tpin", tpin)
                    .put("userID", userID);

            JsonObjectRequest rq = new JsonObjectRequest(
                    Request.Method.POST,
                    userUrl + "/set_tpin",
                    params,
                    resp -> {
                        Toast.makeText(this, "TPIN set successfully!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        finish();
                    },
                    err -> Toast.makeText(this, "Failed to set TPIN", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(rq);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error setting TPIN", Toast.LENGTH_SHORT).show();
        }
    }

    private EditText[] createBoxes(Dialog d, int layoutId, int count) {
        LinearLayout layout = d.findViewById(layoutId);
        layout.removeAllViews();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);

        EditText[] boxes = new EditText[count];
        for (int i = 0; i < count; i++) {
            EditText b = new EditText(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(8, 8, 8, 8);
            b.setLayoutParams(p);
            b.setInputType(InputType.TYPE_CLASS_NUMBER);
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(R.drawable.box_background);
            boxes[i] = b;

            final int idx = i;
            b.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int b, int a) {}
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < count - 1) boxes[idx + 1].requestFocus();
                }
            });
            layout.addView(b);
        }
        boxes[0].requestFocus();
        return boxes;
    }

    private String getBoxValue(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText b : boxes) sb.append(b.getText().toString().trim());
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (behaviorMonitor != null) behaviorMonitor.stopMonitoring();
    }
}
