package com.surakshak.app;

import android.app.Activity;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class PanicGesture2 {


    public static boolean isLoginPanicPin(String input, String mobile) {
        return input.equals(mobile.substring(0, 6));
    }

    // For notification icon: long press 5s
    public static void handleNotificationGesture(View notifIcon, Activity activity) {
        notifIcon.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                new Handler().postDelayed(() -> {
                    if (v.isPressed()) {
                        PanicUtils.triggerPanicLock(activity);
                    }
                }, 5000);
            }
            return false;
        });
    }

    // For TPIN: 5s hold
    public static void handleTPINTapGesture2(View editTextPIN, Activity activity) {
        editTextPIN.setOnLongClickListener(view -> {
            // Start a 5-second delayed trigger on long click
            new Handler().postDelayed(() -> PanicUtils.triggerPanicLock(activity), 5000);
            return true; // Consume the long click event
        });
    }

}
