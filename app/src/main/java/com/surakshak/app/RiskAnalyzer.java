package com.surakshak.app;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.surakshak.app.utils.Constants.RISK_API;

public class RiskAnalyzer {
    private static final String TAG = "RiskAnalyzer";

    public static void evaluateRisk(Context context, JSONObject behaviorVector) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                RISK_API,
                behaviorVector,
                response -> {
                    double riskScore = response.optDouble("riskScore", -1);
                    Log.d(TAG, "🧠 Risk Score: " + riskScore);
                },
                error -> {
                    Log.e(TAG, "Risk evaluation failed: " + error.toString());
                }
        );
        Volley.newRequestQueue(context).add(request);
    }
}
