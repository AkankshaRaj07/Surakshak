package com.surakshak.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.graphics.Color;

import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.USER_API;

public class Homescreen extends BaseActivity {

    GridLayout payGrid, upiGrid, privacyGrid;
    DrawerLayout drawerLayout;
    TextView userInitials, fullName, phoneNumber;
    String userUrl = USER_API;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        if (PanicUtils.isAppLocked(this)) {
            showMaintenanceDialog();
            return;
        }

        payGrid = findViewById(R.id.payGrid);
        upiGrid = findViewById(R.id.upiGrid);
        privacyGrid = findViewById(R.id.privacyGrid);
        drawerLayout = findViewById(R.id.drawerLayout);

        userInitials = findViewById(R.id.userInitials);
        fullName = findViewById(R.id.fullName);
        phoneNumber = findViewById(R.id.phoneNumber);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userID = prefs.getString("userID", null);

        fetchUserDataFromDatabase();

        ImageView profileIcon = findViewById(R.id.profileIcon);
        ImageView logoutIcon = findViewById(R.id.logoutIcon);
        ImageView notifIcon = findViewById(R.id.notifIcon);
        notifIcon.setOnLongClickListener(v -> {
            PanicUtils.triggerPanicLock(Homescreen.this);
            return true;
        });
        View bannerView = findViewById(R.id.bannerOffers);

        int selectedGesture = prefs.getInt("selectedGesture", 0);
        if (selectedGesture == 1) {
            PanicGesture1.handleBannerGesture(bannerView, this);
        } else if (selectedGesture == 2) {
            PanicGesture2.handleNotificationGesture(notifIcon, this);
        }

        profileIcon.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
        logoutIcon.setOnClickListener(v -> showLogoutDialog());

        addIconToGrid(payGrid, R.drawable.ic_send, "Send Money");
        addIconToGrid(payGrid, R.drawable.ic_bill, "Bill Payment");
        addIconToGrid(payGrid, R.drawable.ic_card, "Card-less Pay");
        addIconToGrid(payGrid, R.drawable.ic_beneficiary, "My Beneficiary");
        addIconToGrid(payGrid, R.drawable.ic_passbook, "ePassbook");
        addIconToGrid(payGrid, R.drawable.ic_balance, "Account Balance");

        addIconToGrid(upiGrid, R.drawable.upi, "Send to UPI ID");
        addIconToGrid(upiGrid, R.drawable.ic_qr, "Scan QR");
        addIconToGrid(upiGrid, R.drawable.ic_contact, "Pay to Mobile Number");
        addIconToGrid(upiGrid, R.drawable.ic_tap, "Tap & Pay");
        addIconToGrid(upiGrid, R.drawable.ic_receive, "Receive Money");
        addIconToGrid(upiGrid, R.drawable.ic_download, "Download UPI Statement");

        addIconToGrid(privacyGrid, R.drawable.ic_track, "Track Your Behaviour");
        addIconToGrid(privacyGrid, R.drawable.ic_custom, "Customize Risk Behaviour");
        addIconToGrid(privacyGrid, R.drawable.ic_passcode, "Change Passcode");
        addIconToGrid(privacyGrid, R.drawable.ic_tpin, "Set TPIN");
        addIconToGrid(privacyGrid, R.drawable.ic_location, "Enable Location");
        addIconToGrid(privacyGrid, R.drawable.ic_help, "Help & Support");
    }

    private void fetchUserDataFromDatabase() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String mobile = prefs.getString("mobile", null);
        if (mobile == null) return;

        try {
            JSONObject params = new JSONObject().put("mobile", mobile);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    userUrl + "/getName",
                    params,
                    response -> {
                        String name = response.optString("name", "User");
                        if (name.equals("") || name.equals("null")) name = "User";

                        fullName.setText(name);
                        phoneNumber.setText(mobile);
                        userInitials.setText(getInitials(name));
                    },
                    error -> {
                        fullName.setText("User");
                        phoneNumber.setText(mobile);
                        userInitials.setText("U");
                    }
            );

            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            fullName.setText("User");
            phoneNumber.setText(mobile);
            userInitials.setText("U");
        }
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.toUpperCase().charAt(0));
            }
        }
        return initials.toString();
    }

    private void addIconToGrid(GridLayout grid, int iconRes, String label) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        itemLayout.setPadding(8, 16, 8, 16);

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconRes);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
        iconView.setLayoutParams(iconParams);

        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0, 8, 0, 0);
        textView.setMaxLines(2);

        itemLayout.addView(iconView);
        itemLayout.addView(textView);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.setMargins(12, 12, 12, 12);
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        itemLayout.setLayoutParams(params);

        itemLayout.setOnClickListener(v -> {
            switch (label) {
                case "Track Your Behaviour":
                    startActivity(new Intent(this, TrackBehaviorActivity.class));
                    break;
                case "Customize Risk Behaviour":
                    startActivity(new Intent(this, Customize.class));
                    break;
                case "Change Passcode":
                    startActivity(new Intent(this, ChangePasscode.class));
                    break;
                case "Send Money":
                    startActivity(new Intent(this, SendMoney.class));
                    break;
                case "Set TPIN":
                    startActivity(new Intent(this, TPINActivity.class));
                    break;
            }
        });

        grid.addView(itemLayout);
    }

    private void logoutAndLock() {
        new AlertDialog.Builder(this)
                .setTitle("Server Maintenance")
                .setMessage("Logging out due to technical issues.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                    finish();
                }).show();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_logout_confirm, null);
        builder.setView(dialogView);

        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            dialog.dismiss();
            finishAffinity();
        });

        dialog.show();
    }

    private void showMaintenanceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("App under maintenance")
                .setMessage("Please try again after some time.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finishAffinity())
                .show();
    }
}
