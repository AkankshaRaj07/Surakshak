package com.surakshak.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FakeHomescreen extends AppCompatActivity {

    GridLayout payGrid, upiGrid, privacyGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        payGrid = findViewById(R.id.payGrid);
        upiGrid = findViewById(R.id.upiGrid);
        privacyGrid = findViewById(R.id.privacyGrid);

        // 🔵 Logout icon click
        ImageView logoutIcon = findViewById(R.id.logoutIcon); // Ensure this is in layout XML
        logoutIcon.setOnClickListener(v -> showLogoutDialog());

        // Add icons to Pay & Transfer
        addIconToGrid(payGrid, R.drawable.ic_send, "Send Money");
        addIconToGrid(payGrid, R.drawable.ic_bill, "Bill Payment");
        addIconToGrid(payGrid, R.drawable.ic_card, "Card-less Pay");
        addIconToGrid(payGrid, R.drawable.ic_beneficiary, "My Beneficiary");
        addIconToGrid(payGrid, R.drawable.ic_passbook, "ePassbook");
        addIconToGrid(payGrid, R.drawable.ic_balance, "Account Balance");

        // Add icons to UPI
        addIconToGrid(upiGrid, R.drawable.upi, "Send to UPI ID");
        addIconToGrid(upiGrid, R.drawable.ic_qr, "Scan QR");
        addIconToGrid(upiGrid, R.drawable.ic_contact, "Pay to Mobile Number");
        addIconToGrid(upiGrid, R.drawable.ic_tap, "Tap & Pay");
        addIconToGrid(upiGrid, R.drawable.ic_receive, "Receive Money");
        addIconToGrid(upiGrid, R.drawable.ic_download, "Download UPI Statement");

        // Add icons to Privacy Dashboard
        addIconToGrid(privacyGrid, R.drawable.ic_track, "Track Your Behaviour");
        addIconToGrid(privacyGrid, R.drawable.ic_custom, "Customize Risk Behavior");
        addIconToGrid(privacyGrid, R.drawable.ic_passcode, "Change Passcode");
        addIconToGrid(privacyGrid, R.drawable.ic_tpin, "Set TPIN");
        addIconToGrid(privacyGrid, R.drawable.ic_location, "Enable Location");
        addIconToGrid(privacyGrid, R.drawable.ic_help, "Help & Support");
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
        textView.setEllipsize(null);

        itemLayout.addView(iconView);
        itemLayout.addView(textView);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.setMargins(12, 12, 12, 12);
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        itemLayout.setLayoutParams(params);

        grid.addView(itemLayout);
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
            SharedPreferences prefs = getSharedPreferences("SurakshaPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("isLoggedIn", false).apply();
            dialog.dismiss();
            finishAffinity(); // Close all activities and exit app
        });

        dialog.show();
    }
}
