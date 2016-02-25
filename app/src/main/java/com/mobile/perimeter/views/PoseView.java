package com.mobile.perimeter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mobile.perimeter.R;
import com.mobile.perimeter.util.Conversions;
import com.mobile.perimeter.util.PoseInfo;

public class PoseView extends View {
	protected static final String TAG = "PoseView";

	private Paint mColour = new Paint();
	private static int DIST_NUMBERS_APART = 11;
	private Handler mPoseDisplayHandler = new Handler();
	private int mFrameRate = 200;
	private boolean mNewPoseAvailable = false;

	public PoseView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub

	}

	public PoseView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onDraw(Canvas c) {
		mColour.setTextSize(25);

		mColour.setColor(getResources().getColor(R.color.cheery_pink));
		c.drawText("X: " + formatString(Conversions.inchesToCm(PoseInfo.translationX)), 5,
				DIST_NUMBERS_APART * 3, mColour);

		c.drawText("Y: " + formatString(Conversions.inchesToCm(PoseInfo.translationY)), 5,
				DIST_NUMBERS_APART * 5, mColour);

		c.drawText("Z: " + formatString(Conversions.inchesToCm(PoseInfo.translationZ)), 5,
				DIST_NUMBERS_APART * 7, mColour);

		mColour.setColor(getResources().getColor(R.color.apple_yellow));
		c.drawText("R: " + formatString(PoseInfo.rotationRoll), 5, DIST_NUMBERS_APART * 9,
				mColour);

		c.drawText("P: " + formatString(PoseInfo.rotationPitch), 5, DIST_NUMBERS_APART * 11,
				mColour);

		c.drawText("Y: " + formatString(PoseInfo.rotationYaw), 5, DIST_NUMBERS_APART * 13,
				mColour);

	}

	private String formatString(double value) {
		return String.format("%.2f", value);
	}

	public void startPoseDisplay() {
		mPoseDisplayHandler.postDelayed(mPoseLoop, mFrameRate);

	}

	public void stopPoseDisplay() {
		Log.i(TAG, "pose display stopped");
		mPoseDisplayHandler.removeCallbacks(mPoseLoop);

	}

	private Runnable mPoseLoop = new Runnable() {
		@Override
		public void run() {

			if (mNewPoseAvailable) {
				mNewPoseAvailable = false;
				invalidate(); // Update visuals (ends up calling onDraw)
			}

			// Delay the next update by FRAME_RATE milliseconds
			mPoseDisplayHandler.postDelayed(mPoseLoop, mFrameRate);

		}
	};

	public void refreshPoseEstimation() {
		Log.i(TAG, "x: " + Conversions.inchesToCm(PoseInfo.translationX) + " y: " + Conversions.inchesToCm(PoseInfo.translationY) + " z: "
				+ Conversions.inchesToCm(PoseInfo.translationZ) + " r: " + PoseInfo.rotationRoll + " p: " + PoseInfo.rotationPitch + " y: "
				+ PoseInfo.rotationYaw);
		mNewPoseAvailable = true;
	}
}
