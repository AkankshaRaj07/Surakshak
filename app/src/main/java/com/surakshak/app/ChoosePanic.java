package com.surakshak.app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ChoosePanic extends AppCompatActivity {

    private CheckBox gesture1CheckBox, gesture2CheckBox;
    private Button proceedButton;
    private boolean selectionLocked = false; // to prevent changing after selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPanicGestureDialog();
    }

    private void showPanicGestureDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_choose_panic);
        dialog.setCancelable(false);

        gesture1CheckBox = dialog.findViewById(R.id.gesture1);
        gesture2CheckBox = dialog.findViewById(R.id.gesture2);
        proceedButton = dialog.findViewById(R.id.btnProceedGesture);

        // Lock selection after first choice
        gesture1CheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectionLocked) {
                    gesture2CheckBox.setChecked(false);
                    showLockAlert();
                    selectionLocked = true;
                } else {
                    gesture1CheckBox.setChecked(true);
                    gesture2CheckBox.setChecked(false);
                }
            }
        });

        gesture2CheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectionLocked) {
                    gesture1CheckBox.setChecked(false);
                    showLockAlert();
                    selectionLocked = true;
                } else {
                    gesture2CheckBox.setChecked(true);
                    gesture1CheckBox.setChecked(false);
                }
            }
        });

        proceedButton.setOnClickListener(v -> {
            if (!gesture1CheckBox.isChecked() && !gesture2CheckBox.isChecked()) {
                Toast.makeText(this, "Please select one panic gesture", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("SurakshaPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            int selected = gesture1CheckBox.isChecked() ? 1 : 2;
            editor.putInt("selectedGesture", selected);
            editor.apply();

            showGestureDetailsDialog(selected);  // now show second dialog
            dialog.dismiss();  // close first dialog
        });

        dialog.show();
    }

    private void showLockAlert() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Attention")
                .setMessage("As you proceed, you cannot choose another panic gesture.")
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showGestureDetailsDialog(int selectedGesture) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_gesture_info);
        dialog.setCancelable(false);

        TextView infoText = dialog.findViewById(R.id.gestureInfoText);
        Button gotIt = dialog.findViewById(R.id.btnGotIt);
        gotIt.setEnabled(false);

        if (selectedGesture == 1) {
            infoText.setText("Note: The 'Got It' button will be enabled after 30 seconds. Please read the instructions carefully.\n\n" +
                    "Panic Gesture 1 has been enabled.\n\n" +
                    "You can now trigger a silent security lock using the following actions:\n\n" +
                    "• If you are at Login Screen: Enter the last 6 digits of your registered mobile number instead of your passcode.\n\n" +
                    "• On Home Screen: Tap the template box above Pay and Transfer section twice, then hold for 3 seconds on the third tap.\n\n" +
                    "• While Entering TPIN: Tap the TPIN field twice, then hold on the third tap for 3 seconds.\n\n" +
                    "After triggering this gesture, your app will lock for 10 minutes and cannot be accessed during that period.\n" +
                    "Please use this feature only in case of emergency.");
        } else {
            infoText.setText("Note: The 'Got It' button will be enabled after 30 seconds. Please read the instructions carefully.\n\n" +
                    "Panic Gesture 2 has been enabled.\n\n" +
                    "You can now trigger a silent security lock using the following actions:\n\n" +
                    "• If you are at Login Screen: Enter the first 6 digits of your registered mobile number instead of your passcode.\n\n" +
                    "• On Home Screen: Hold the notification icon for 5 seconds.\n\n" +
                    "• While Entering TPIN: Tap and hold the TPIN field for 5 seconds.\n\n" +
                    "After triggering this gesture, your app will lock for 10 minutes and cannot be accessed during that period.\n" +
                    "Please use this feature only in case of emergency.");
        }



        new Handler().postDelayed(() -> gotIt.setEnabled(true), 30000);

        gotIt.setOnClickListener(v -> {
            dialog.dismiss();
            showMLInfoDialog(); // Final info dialog
        });

        dialog.show();
    }

    private void showMLInfoDialog() {
        new AlertDialog.Builder(ChoosePanic.this)
                .setTitle("Let’s Train Your Digital Bodyguard!")
                .setMessage("Your app is learning how you swipe, type, and move —\nso it can spot intruders even after login.\n\n1) Tracks: Swipe speed | Typing style | Phone motion\n2) Data stays only on your phone\n3) No messages, passwords, or personal files are touched\n\nJust use the app like normal — protection starts now!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    startActivity(new Intent(ChoosePanic.this, TrainingSessionActivity.class));
                    finish();
                })
                .show();
    }
}
