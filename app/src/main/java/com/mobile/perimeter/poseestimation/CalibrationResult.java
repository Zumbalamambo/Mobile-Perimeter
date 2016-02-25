package com.mobile.perimeter.poseestimation;

import org.opencv.core.Mat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

public abstract class CalibrationResult {
	private static final String TAG = "CalibrationResult";

	private static final int CAMERA_MATRIX_ROWS = 3;
	private static final int CAMERA_MATRIX_COLS = 3;
	private static final int DISTORTION_COEFFICIENTS_SIZE = 5;
	public static final String CAMERA_CALIBRATION_DATA = "CameraCalibrationData";

	public static void storeCameraData(Activity activity) {
		SharedPreferences sharedPref = activity.getSharedPreferences(
				CAMERA_CALIBRATION_DATA, 0);
		SharedPreferences.Editor editor = sharedPref.edit();

		double[] cameraMatrixArray = { 1107.022127, 0, 639.5, 0,
				1107.022127, 383.5, 0, 0, 1 };
		for (int i = 0; i < CAMERA_MATRIX_ROWS; i++) {
			for (int j = 0; j < CAMERA_MATRIX_COLS; j++) {
				Integer id = i * CAMERA_MATRIX_ROWS + j;
				editor.putFloat(id.toString(), (float) cameraMatrixArray[id]);
			}
		}

		double[] distortionCoefficientsArray = { -0.0104545663,
				-0.40365614337, 0, 0, 1.637600556 };
		int shift = CAMERA_MATRIX_ROWS * CAMERA_MATRIX_COLS;
		for (Integer i = shift; i < DISTORTION_COEFFICIENTS_SIZE + shift; i++) {
			editor.putFloat(i.toString(), (float) distortionCoefficientsArray[i
					- shift]);
		}

		editor.commit();

		Log.i(TAG, "Calibration results stored");

	}

	public static void loadCameraData(Activity activity, Mat cameraMatrix,
			Mat distortionCoefficients) {

		SharedPreferences sharedPref = activity.getSharedPreferences(
				CAMERA_CALIBRATION_DATA, 0);

		double[] cameraMatrixArray = new double[CAMERA_MATRIX_ROWS
				* CAMERA_MATRIX_COLS];
		for (int i = 0; i < CAMERA_MATRIX_ROWS; i++) {
			for (int j = 0; j < CAMERA_MATRIX_COLS; j++) {
				Integer id = i * CAMERA_MATRIX_ROWS + j;
				cameraMatrixArray[id] = sharedPref.getFloat(id.toString(), -1);
			}
		}
		cameraMatrix.put(0, 0, cameraMatrixArray);
		Log.i(TAG, "Loaded camera matrix: " + cameraMatrix.dump());

		double[] distortionCoefficientsArray = new double[DISTORTION_COEFFICIENTS_SIZE];
		int shift = CAMERA_MATRIX_ROWS * CAMERA_MATRIX_COLS;
		for (Integer i = shift; i < DISTORTION_COEFFICIENTS_SIZE + shift; i++) {
			distortionCoefficientsArray[i - shift] = sharedPref.getFloat(
					i.toString(), -1);
		}
		distortionCoefficients.put(0, 0, distortionCoefficientsArray);
		Log.i(TAG,
				"Loaded distortion coefficients: "
						+ distortionCoefficients.dump());

	}

}
