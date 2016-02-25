package com.mobile.perimeter.light;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.Conversions;
import com.mobile.perimeter.util.Debugging;
import com.mobile.perimeter.util.PoseInfo;
import com.mobile.perimeter.util.ScreenInfo;

public final class LightUtil {
	protected static final String TAG = "LightUtil";

	SensorManager mySensorManager;
	Sensor mLightSensor;
	private static int mLux;
	private static double mSlope;
	private static double mIntercept;
	private static double mAngle;
	private static final double GAMMA_CORRECTION = 2.2;
	private static final double ROUND_INT = 0.5;
	private static boolean mAngleValid = false;

	public LightUtil(Context context) {
		mySensorManager = (SensorManager) context
				.getSystemService(context.SENSOR_SERVICE);

		mLightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

		if (mLightSensor != null) {
			mySensorManager.registerListener(LightSensorListener, mLightSensor,
					SensorManager.SENSOR_DELAY_NORMAL);

		}
	}

	private final SensorEventListener LightSensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
				mLux = Math.round(event.values[0]);

				// Log.i(TAG,"light value = " + mLux);
			}
		}

	};

	private static double findAngle(int stimPosX, int stimPosY) {

		double tX, tY, tZ;

		// For testing purposes, if calibration mode is 0, user wants to track the head, but not calibrate the intensity/position of stimuli according
		// to the position of the head, so reset them back to "default", else, just keep it as is (in head tracking mode, it will continue to be
		// the position estimated, otherwise it's the "default")
		if (!Consts.USE_ESTIMATOR_MODE) {
			tX = PoseInfo.CONST_TRANSLATION_X_INCHES;
			tY = PoseInfo.CONST_TRANSLATION_Y_INCHES;
			tZ = PoseInfo.CONST_TRANSLATION_Z_INCHES;
		} else {
			tX = PoseInfo.translationX;
			tY = PoseInfo.translationY;
			tZ = PoseInfo.translationZ;
		}

		double eyePosX = Conversions.toPixels(tX, ScreenInfo.xDPI);
		double eyePosY = Conversions.toPixels(tY, ScreenInfo.yDPI);
		double eyePosZ = Conversions.toPixels(tZ, ScreenInfo.xDPI);

		double angleRadians = Math
				.asin((Math.abs(eyePosZ))
						/ (Math.sqrt(Math.pow(stimPosX - eyePosX, 2)
								+ Math.pow(stimPosY - eyePosY, 2) + eyePosZ
								* eyePosZ)));

		double angle = 90.0 - Math.toDegrees(angleRadians);

		Log.i(TAG,"angle between line and plane: " + angle);
		Debugging.appendLog("EyePos: " + eyePosX + "," + eyePosY + ","
				+ eyePosZ);
		Debugging.appendLog("StimPos: " + stimPosX + "," + stimPosY);
		Debugging.appendLog("Angle found between line and tablet: " + angle);
		if (angle < 30) {
			mAngleValid = true;
		} else {
			mAngleValid = false;
		}
		return angle;

	}

	private static double adjustLuminanceAccordingToLux() {
		/*
		 * double angleAddition = 0; if (mAngle >= 5) { angleAddition =
		 * 0.0988*mLux-0.0294; }
		 */
		// return 0.1111*mLux-0.2543;
		return 0.0009 * mLux * mLux + 0.0039 * mLux + 0.8;
		// return 0.0123*mLux-0.2249 + angleAddition;
	}

	private static double calcLuminanceFromRGB(int rgb) {

		return Math.pow(rgb * mSlope + mIntercept, GAMMA_CORRECTION)
				+ adjustLuminanceAccordingToLux();

	}

	private static void calculateScreenCharacterizingEquation() {
		mSlope = -0.00002 * mAngle * mAngle - 0.0002 * mAngle + 0.0621;
		// mSlope = 0.0622;
		mIntercept = 0.5; // 0.0001*mLux*mLux+0.0009*mLux+0.4376;
		Log.i(TAG, "lux = " + mLux + "m = " + mSlope + " b = " + mIntercept);
		Debugging.appendLog("Current Lux: " + mLux);
		Debugging.appendLog("m = " + mSlope + " b = " + mIntercept);
	}

	public static int calculateRGB(int stimPosX, int stimPosY, int dB) {

		mAngle = findAngle(stimPosX, stimPosY);

		calculateScreenCharacterizingEquation();

		double wantedL = Math.pow(10, (25 - (double) dB) / 10);
		double backgroundLuminance;
		double luminance;
		int rgb;
		if (Consts.LIGHTING) {
			backgroundLuminance = calcLuminanceFromRGB(37);
			luminance = (backgroundLuminance * wantedL + backgroundLuminance)
					- adjustLuminanceAccordingToLux();

			rgb = (int) ((Math.pow(luminance, (1 / GAMMA_CORRECTION)) - mIntercept)
					/ mSlope + ROUND_INT);

		} else {
			backgroundLuminance = 7;
			luminance = backgroundLuminance * wantedL + backgroundLuminance;
			rgb = (int) ((Math.pow(luminance, (1 / GAMMA_CORRECTION)) - 0.4275) / 0.0617 + ROUND_INT);

		}

		Log.i(TAG, "background l = " + backgroundLuminance
				+ "colour calculated: " + rgb + " Db:  " + dB);
		Debugging.appendLog("Background luminance = " + backgroundLuminance
				+ " Colour calculated: " + rgb);
		Debugging.appendLog("dB = " + dB);
		return rgb;

	}

	public void stopLightSensor() {
		mySensorManager.unregisterListener(LightSensorListener);
	}

	public static double getLux() {
		return mLux;
	}

	public static boolean isAngleValid() {
		return mAngleValid;
	}
}
