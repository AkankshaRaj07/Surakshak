package com.surakshak.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.RISK_API;

public class BaseActivity extends AppCompatActivity {

    private BehaviorMonitor baseBehaviorMonitor;
    private Handler baseRiskHandler;
    private Runnable baseRiskRunnable;
    private String userID;
    private boolean isRedirecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userID = prefs.getString("userID", null);

        baseBehaviorMonitor = new BehaviorMonitor(this);
        baseBehaviorMonitor.startMonitoring();

        baseRiskHandler = new Handler();
        baseRiskRunnable = new Runnable() {
            @Override
            public void run() {
                if (userID != null && !isRedirecting) {
                    JSONObject vector = baseBehaviorMonitor.getBehaviorVectorAsJSON(userID);
                    if (vector != null) {
                        JsonObjectRequest riskRequest = new JsonObjectRequest(
                                Request.Method.POST,
                                RISK_API,
                                vector,
                                response -> {
                                    String level = response.optString("risk_level", "low");
                                    Log.d("BaseRiskEval", "Risk level = " + level);
                                    handleRiskLevel(level);
                                },
                                error -> Log.e("BaseRiskEval", "Error: " + error.toString())
                        );
                        Volley.newRequestQueue(BaseActivity.this).add(riskRequest);
                    }
                }
                baseRiskHandler.postDelayed(this, 20000);
            }
        };
        baseRiskHandler.postDelayed(baseRiskRunnable, 20000);
    }

    private void handleRiskLevel(String level) {
        if (level.equals("medium")) {
            isRedirecting = true;
            Intent i = new Intent(this, ReauthenticationActivity.class);
            i.putExtra("risk_level", "medium");
            startActivity(i);
        } else if (level.equals("high")) {
            isRedirecting = true;
            Intent i = new Intent(this, FakeHomescreen.class);
            i.putExtra("risk_level", "high");
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (baseBehaviorMonitor != null) baseBehaviorMonitor.stopMonitoring();
        if (baseRiskHandler != null) baseRiskHandler.removeCallbacks(baseRiskRunnable);
    }
}
