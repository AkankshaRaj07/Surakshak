package com.surakshak.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;

public class BehaviorMonitor implements SensorEventListener {

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor accelSensor, gyroSensor;

    private final ArrayList<Long> dwellTimes = new ArrayList<>();
    private final ArrayList<Long> flightTimes = new ArrayList<>();
    private final ArrayList<Float> pressures = new ArrayList<>();
    private final ArrayList<Float> sizes = new ArrayList<>();
    private final ArrayList<Long> interKeyDelays = new ArrayList<>();
    private final ArrayList<Float> accelValues = new ArrayList<>();
    private final ArrayList<Float> gyroValues = new ArrayList<>();

    private long lastKeyDownTime = 0;
    private long lastKeyUpTime = 0;
    private long lastTouchTime = 0;
    private long typingStartTime = 0;
    private long typingEndTime = 0;
    private long screenHoldStartTime = 0;
    private long screenHoldEndTime = 0;

    private final Handler handler = new Handler();
    private final Runnable logBehaviorRunnable = new Runnable() {
        @Override
        public void run() {
            logBehaviorVector();
            handler.postDelayed(this, 20000); // every 20s
        }
    };

    private boolean isMonitoring = false;

    public BehaviorMonitor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void startMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        screenHoldStartTime = System.currentTimeMillis();
        handler.postDelayed(logBehaviorRunnable, 20000);
    }

    public void stopMonitoring() {
        if (!isMonitoring) return;
        isMonitoring = false;
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(logBehaviorRunnable);
    }

    public void reset() {
        dwellTimes.clear();
        flightTimes.clear();
        pressures.clear();
        sizes.clear();
        interKeyDelays.clear();
        accelValues.clear();
        gyroValues.clear();
        typingStartTime = 0;
        typingEndTime = 0;
        screenHoldStartTime = System.currentTimeMillis();
    }

    public void trackTouch(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                pressures.add(event.getPressure());
                sizes.add(event.getSize());
                if (lastTouchTime != 0) {
                    interKeyDelays.add(now - lastTouchTime);
                }
                lastTouchTime = now;
            }
            return false;
        });
    }

    public void attachToEditText(EditText editText) {
        editText.setOnKeyListener((v, keyCode, event) -> {
            long now = System.currentTimeMillis();
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if (lastKeyUpTime != 0) {
                        flightTimes.add(now - lastKeyUpTime);
                    }
                    lastKeyDownTime = now;
                    if (typingStartTime == 0) typingStartTime = now;
                    break;
                case KeyEvent.ACTION_UP:
                    if (lastKeyDownTime != 0) {
                        dwellTimes.add(now - lastKeyDownTime);
                    }
                    lastKeyUpTime = now;
                    typingEndTime = now;
                    break;
            }
            return false;
        });
    }

    public JSONObject getBehaviorVectorAsJSON(String userID) {
        screenHoldEndTime = System.currentTimeMillis();
        long screenHoldTime = screenHoldEndTime - screenHoldStartTime;

        double typingSpeed = 0.0;
        if (typingStartTime != 0 && typingEndTime > typingStartTime) {
            long duration = typingEndTime - typingStartTime;
            typingSpeed = duration > 0 ? (dwellTimes.size() / (duration / 1000.0)) : 0.0;
        }

        double avgDwellTime = dwellTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgFlightTime = flightTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgPressure = pressures.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        double avgSize = sizes.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        double avgInterKeyDelay = interKeyDelays.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgAccel = accelValues.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        double avgGyro = gyroValues.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);

        try {
            JSONObject obj = new JSONObject();
            obj.put("userID", userID);
            obj.put("TypingSpeed", typingSpeed);
            obj.put("DwellTime", avgDwellTime);
            obj.put("FlightTime", avgFlightTime);
            obj.put("InterKeyDelay", avgInterKeyDelay);
            obj.put("TapPressure", avgPressure);
            obj.put("TouchSize", avgSize);
            obj.put("TiltAngle", avgAccel);
            obj.put("GyroPattern", avgGyro);
            obj.put("ScreenHoldTime", screenHoldTime);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            reset();
        }
    }

    private void logBehaviorVector() {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userID = prefs.getString("userID", "debug");
        JSONObject vec = getBehaviorVectorAsJSON(userID);
        if (vec != null)
            Log.d("BehaviorMonitor", "Vector=" + vec.toString());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float totalAccel = (float) Math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]);
            accelValues.add(totalAccel);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float totalGyro = (float) Math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]);
            gyroValues.add(totalGyro);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
