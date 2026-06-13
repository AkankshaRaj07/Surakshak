package com.surakshak.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.USER_API;
import static com.surakshak.app.utils.Constants.BASE_IP;

public class Passcode extends AppCompatActivity {
    LinearLayout passcodeLayout;
    Button btnProceed;
    EditText[] passcodeBoxes;
    TextView subText;
    String mobile, name, passcode = "";
    final int BOX_COUNT = 6;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_passcode);

        mobile = getIntent().getStringExtra("mobile");
        name = getIntent().getStringExtra("name");
        userID = getIntent().getStringExtra("userID");  // <-- get userID from intent here

        passcodeLayout = findViewById(R.id.passcodeBoxLayout);
        btnProceed = findViewById(R.id.btnProceed);
        subText = findViewById(R.id.subText);

        subText.setText("Enter passcode");
        subText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

        createBoxes(passcodeLayout, BOX_COUNT);

        btnProceed.setOnClickListener(v -> {
            passcode = getBoxValue(passcodeBoxes);
            if (passcode.length() == BOX_COUNT) {
                savePasscodeToDB(mobile, passcode, name, userID);
            } else {
                Toast.makeText(this, "Enter 6-digit passcode", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Removed redundant sendOtpAgain and verifyFinalOtp methods

    private void savePasscodeToDB(String mobile, String passcode, String name, String userID) {
        try {
            JSONObject params = new JSONObject();
            params.put("mobile", mobile);
            params.put("passcode", passcode);
            params.put("name", name);
            params.put("userID", userID);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    USER_API + "/register",
                    params,
                    response -> {
                        Toast.makeText(this, "🎉 Passcode saved!", Toast.LENGTH_SHORT).show();

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("mobile", mobile)
                                .putString("userID", userID)
                                .apply();

                        Intent intent = new Intent(Passcode.this, ChoosePanic.class);
                        intent.putExtra("mobile", mobile);
                        intent.putExtra("userID", userID);
                        startActivity(intent);
                        finish();
                    },
                    error -> {
                        String errorMsg = error.networkResponse != null && error.networkResponse.data != null
                                ? new String(error.networkResponse.data)
                                : "Unknown error";
                        Toast.makeText(this, "❌ Failed to save passcode", Toast.LENGTH_SHORT).show();
                        Log.e("SAVE_PASSCODE_ERROR", errorMsg);
                    }
            );

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createBoxes(LinearLayout layout, int count) {
        if (passcodeBoxes != null && passcodeBoxes.length == count) return;

        layout.removeAllViews();
        EditText[] boxes = new EditText[count];

        for (int i = 0; i < count; i++) {
            EditText box = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 150);
            params.setMargins(10, 0, 10, 0);
            box.setLayoutParams(params);
            box.setBackgroundResource(R.drawable.box_background);
            box.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            box.setTextSize(24);
            box.setInputType(InputType.TYPE_CLASS_NUMBER);
            box.setEms(1);
            box.setId(View.generateViewId());

            int finalI = i;
            box.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && finalI < count - 1) {
                        passcodeBoxes[finalI + 1].requestFocus();
                    }
                }
            });

            layout.addView(box);
            boxes[i] = box;
        }

        passcodeBoxes = boxes;
        boxes[0].requestFocus();
    }

    private String getBoxValue(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText box : boxes) {
            sb.append(box.getText().toString().trim());
        }
        return sb.toString();
    }
}
