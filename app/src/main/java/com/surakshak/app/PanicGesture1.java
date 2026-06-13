package com.surakshak.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class PanicGesture1 {

    private static long[] tapTimestamps = new long[3];
    private static int tapIndex = 0;


    public static boolean isLoginPanicPin(String input, String mobile) {
        return input.equals(mobile.substring(mobile.length() - 6));
    }

    // For gesture on banner: 2 taps + hold
    public static void handleBannerGesture(View bannerOffers, Activity activity) {
        bannerOffers.setOnClickListener(v -> {
            tapTimestamps[tapIndex] = System.currentTimeMillis();
            tapIndex = (tapIndex + 1) % 3;
        });

        bannerOffers.setOnLongClickListener(view -> {
            if (tapTimestamps[2] - tapTimestamps[0] <= 5000) {
                PanicUtils.triggerPanicLock(activity);
                return true;
            }
            return false;
        });
    }

    // For TPIN: 2 taps + hold on 3rd tap
    public static void handleTPINTapGesture(View editTextPIN, Activity activity) {
        final long[] tapTimestamps = new long[3];
        final int[] tapIndex = {0};

        editTextPIN.setOnClickListener(v -> {
            tapTimestamps[tapIndex[0]] = System.currentTimeMillis();
            tapIndex[0] = (tapIndex[0] + 1) % 3;
        });

        editTextPIN.setOnLongClickListener(view -> {
            if (tapTimestamps[2] - tapTimestamps[0] <= 5000) {
                PanicUtils.triggerPanicLock(activity);
                return true;
            }
            return false;
        });
    }

}
