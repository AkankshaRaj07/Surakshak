package com.surakshak.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.USER_API;

public class SendMoney extends AppCompatActivity {

    private EditText editTextAC, editTextIFSC, editTextRecipient, editTextAmount, editTextMessage, editTextPIN;
    private Button btnSend;
    private String mobile, userID;

    private BehaviorMonitor behaviorMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PanicUtils.isAppLocked(this)) {
            PanicUtils.checkAndShowMaintenance(this);
            return;
        }

        setContentView(R.layout.send_money);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        mobile = prefs.getString("mobile", null);
        userID = prefs.getString("userID", null);

        editTextAC = findViewById(R.id.editTextAC);
        editTextRecipient = findViewById(R.id.editTextRecipient);
        editTextIFSC = findViewById(R.id.editTextIFSC);
        editTextAmount = findViewById(R.id.editTextAmount);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextPIN = findViewById(R.id.editTextPIN);
        btnSend = findViewById(R.id.btnSendMoney);

        int selectedGesture = prefs.getInt("selectedGesture", 0);
        if (selectedGesture == 1) {
            PanicGesture1.handleTPINTapGesture(editTextPIN, this);
        } else {
            PanicGesture2.handleTPINTapGesture2(editTextPIN, this);
        }

        behaviorMonitor = new BehaviorMonitor(this);
        behaviorMonitor.trackTouch(findViewById(android.R.id.content));
        behaviorMonitor.attachToEditText(editTextAC);
        behaviorMonitor.attachToEditText(editTextIFSC);
        behaviorMonitor.attachToEditText(editTextRecipient);
        behaviorMonitor.attachToEditText(editTextAmount);
        behaviorMonitor.attachToEditText(editTextMessage);
        behaviorMonitor.attachToEditText(editTextPIN);
        behaviorMonitor.startMonitoring();

        btnSend.setOnClickListener(v -> {
            String accnumber = editTextAC.getText().toString().trim();
            String recipient = editTextRecipient.getText().toString().trim();
            String ifsc = editTextIFSC.getText().toString().trim();
            String amount = editTextAmount.getText().toString().trim();
            String message = editTextMessage.getText().toString().trim();
            String tpin = editTextPIN.getText().toString().trim();

            if (TextUtils.isEmpty(recipient) || TextUtils.isEmpty(amount) || TextUtils.isEmpty(tpin)) {
                Toast.makeText(this, "All fields including TPIN are required", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyTPIN(tpin, amount, recipient);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (behaviorMonitor != null) behaviorMonitor.stopMonitoring();
    }

    private void verifyTPIN(String tpin, String amount, String recipient) {
        try {
            JSONObject params = new JSONObject();
            params.put("mobile", mobile);
            params.put("tpin", tpin);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    USER_API + "/verify-tpin",
                    params,
                    response -> showSuccessDialog(amount, recipient),
                    error -> showFailureDialog()
            );

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuccessDialog(String amount, String recipient) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_status, null);
        ImageView icon = view.findViewById(R.id.statusIcon);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView desc = view.findViewById(R.id.statusDescription);

        icon.setImageResource(R.drawable.ic_tick_green);
        statusText.setText("Transaction Successful");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        desc.setText("\u20B9" + amount + " sent to " + recipient);

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .show();

        clearFields();
    }

    private void showFailureDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_status, null);
        ImageView icon = view.findViewById(R.id.statusIcon);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView desc = view.findViewById(R.id.statusDescription);

        icon.setImageResource(R.drawable.ic_cross_red);
        statusText.setText("Transaction Failed");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        desc.setText("Incorrect TPIN. Please try again.");

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .show();
    }

    private void clearFields() {
        editTextAC.setText("");
        editTextRecipient.setText("");
        editTextIFSC.setText("");
        editTextAmount.setText("");
        editTextMessage.setText("");
        editTextPIN.setText("");
    }

    public void onBackClick(View view) {
        onBackPressed();
    }
}
