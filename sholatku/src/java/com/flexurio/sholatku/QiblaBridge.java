package com.flexurio.sholatku;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.webkit.JavascriptInterface;

/**
 * Jembatan JS -> kompas kiblat native.
 * JS memanggil start(), lalu polling getAzimuth() (derajat 0-360, -999 bila belum ada data),
 * lalu stop() saat layar kiblat ditutup.
 */
public class QiblaBridge {
    private final Activity act;
    private SensorManager sm;
    private Sensor accel;
    private Sensor magnet;
    private final float[] gravity = new float[3];
    private final float[] geomag = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomag = false;
    private volatile float azimuth = -999f;
    private boolean started = false;

    private final SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3);
                hasGravity = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomag, 0, 3);
                hasGeomag = true;
            }
            if (hasGravity && hasGeomag) {
                float[] R = new float[9];
                float[] I = new float[9];
                if (SensorManager.getRotationMatrix(R, I, gravity, geomag)) {
                    float[] orient = new float[3];
                    SensorManager.getOrientation(R, orient);
                    float az = (float) Math.toDegrees(orient[0]);
                    az = (az + 360f) % 360f;
                    azimuth = az;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    public QiblaBridge(Activity act) {
        this.act = act;
    }

    @JavascriptInterface
    public void start() {
        if (started) return;
        try {
            sm = (SensorManager) act.getSystemService(Context.SENSOR_SERVICE);
            if (sm == null) return;
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (accel == null || magnet == null) return;
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI);
            sm.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI);
            started = true;
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public void stop() {
        if (!started) return;
        try {
            if (sm != null) sm.unregisterListener(listener);
        } catch (Throwable ignored) { }
        started = false;
        azimuth = -999f;
    }

    /** Derajat azimuth HP (0 = utara). -999 bila sensor belum siap / tidak ada. */
    @JavascriptInterface
    public float getAzimuth() {
        return azimuth;
    }
}
