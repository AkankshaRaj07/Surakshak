package com.surakshak.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;


public class PanicUtils {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String PANIC_LOCK_TIME_KEY = "panicLockTime";
    private static final long LOCK_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    public static boolean isAppLocked(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lockUntil = prefs.getLong(PANIC_LOCK_TIME_KEY, 0);
        return System.currentTimeMillis() < lockUntil;
    }

    public static void checkAndShowMaintenance(Activity activity) {
        if (isAppLocked(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle("App under maintenance")
                    .setMessage("Please try again after some time.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> {
                        d.dismiss();
                        activity.finishAffinity(); // Close entire app
                    })
                    .show();
        }
    }

    public static void triggerPanicLock(Activity activity) {
        // Save lock time
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
        prefs.edit().putLong(PANIC_LOCK_TIME_KEY, lockUntil).apply();

        // Show dialog
        new AlertDialog.Builder(activity)
                .setTitle("Banking server is down")
                .setMessage("Please try again after sometime.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    new Handler().postDelayed(activity::finishAffinity, 500); // Delay app close
                })
                .show();
    }
}
