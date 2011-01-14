package com.androsz.electricsleepbeta.app;

import android.R;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class CheckForScreenBugAccelerometerService extends Service implements
		SensorEventListener {

	private final class ScreenReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				serviceHandler.postAtFrontOfQueue(beepRunnable);
				serviceHandler.postDelayed(setScreenIsOffRunnable, 5000);
				serviceHandler.postDelayed(turnScreenOnFallbackRunnable, 12000);
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				stopSelf();
			}
		}
	}

	Handler serviceHandler = new Handler();

	Runnable turnScreenOnFallbackRunnable = new Runnable() {
		@Override
		public void run() {
			turnScreenOn();
		}
	};

	Runnable setScreenIsOffRunnable = new Runnable() {
		@Override
		public void run() {
			screenIsOff = true;
		}
	};

	Runnable beepRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				MediaPlayer mp = MediaPlayer.create(
						CheckForScreenBugAccelerometerService.this,
						com.androsz.electricsleepbeta.R.raw.in_call_alarm);
				if (mp != null) {
					mp.start();
					try {
						java.lang.Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mp.stop();
					mp.release();
				}
			} finally {
				//just fail silently.
			}
		}
	};

	private static final String LOCK_TAG = CheckForScreenBugAccelerometerService.class
			.getName();
	public static final String BUG_NOT_PRESENT = "BUG_NOT_PRESENT";
	public static final String BUG_PRESENT = "BUG_PRESENT";
	private ScreenReceiver screenOnOffReceiver;

	private WakeLock partialWakeLock;

	private boolean bugPresent = true;
	private boolean screenIsOff = false;

	private void obtainWakeLock() {
		final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		partialWakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG);
		partialWakeLock.acquire();
		partialWakeLock.setReferenceCounted(false);
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
		// Not used.
	}

	@Override
	public IBinder onBind(final Intent intent) {
		// Not used
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		screenOnOffReceiver = new ScreenReceiver();
		registerReceiver(screenOnOffReceiver, filter);
		obtainWakeLock();
		registerAccelerometerListener();
	}

	@Override
	public void onDestroy() {
		if (bugPresent) {
			CheckForScreenBugActivity.BUG_PRESENT_INTENT = new Intent(
					BUG_PRESENT);
		}

		unregisterAccelerometerListener();

		unregisterReceiver(screenOnOffReceiver);

		serviceHandler.removeCallbacks(setScreenIsOffRunnable);
		serviceHandler.removeCallbacks(turnScreenOnFallbackRunnable);
		serviceHandler.removeCallbacks(beepRunnable);

		if (partialWakeLock != null && partialWakeLock.isHeld()) {
			partialWakeLock.release();
		}

		super.onDestroy();
	}

	@Override
	public void onSensorChanged(final SensorEvent event) {
		if (screenIsOff && bugPresent) {
			CheckForScreenBugActivity.BUG_PRESENT_INTENT = new Intent(
					BUG_NOT_PRESENT);
			bugPresent = false;
			turnScreenOn();
		}
	}

	private void registerAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	private void turnScreenOn() {
		if (partialWakeLock != null && partialWakeLock.isHeld()) {
			partialWakeLock.release();
		}
		final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final WakeLock wakeLock = powerManager.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.ON_AFTER_RELEASE, LOCK_TAG
						+ java.lang.Math.random());
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire();
		wakeLock.release();
	}

	private void unregisterAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.unregisterListener(this);
	}

}
