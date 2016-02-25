package com.mobile.perimeter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mobile.perimeter.R;
import com.mobile.perimeter.light.LightUtil;
import com.mobile.perimeter.util.Conversions;
import com.mobile.perimeter.util.PoseInfo;
import com.mobile.perimeter.util.ScreenInfo;
import com.mobile.perimeter.views.primitives.CalibrationListener;

public class CalibrationView extends View {
	protected static final String TAG = "CalibrationView";

	private Paint mColour = new Paint();
	private Paint mBackgroundColour = new Paint();
	private static int DIST_NUMBERS_APART = 11;
	private Handler mCalibrationDisplayHandler = new Handler();
	private int mFrameRate = 200;
	private boolean mCloser = false;
	private boolean mFarther = false;
	private boolean mRight = false;
	private boolean mLeft = false;
	private boolean mUp = false;
	private boolean mDown = false;
	private boolean mNeedBetterLighting = false;
	private boolean mUnTiltHead = false;
	private int mCountDownTime = -1;
	private int mCount = 5;
	private boolean stopped = false;
	private int mEvent = 0;

	private static int STATS_X_DIMENSION = 5;

	public CalibrationView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public CalibrationView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private int calculateXDimension(int offset, int xNum, int yNum) {
		if (PoseInfo.deviceFlipped == 0) {
			return xNum;
		} else {
			return yNum * offset;
		}
	}

	private int calculateYDimension(int offset, int xNum, int yNum) {
		if (PoseInfo.deviceFlipped == 0) {
			return yNum * offset;
		} else {
			return ScreenInfo.screenHeight - xNum;
		}
	}

	private void drawTextMaybeFlipped(Canvas c, String text, int x, int y,
			Paint colour) {
		if (PoseInfo.deviceFlipped == 0) {
			c.drawText(text, x, y, colour);
		} else {
			c.save();
			c.rotate(-90, x, y);
			c.drawText(text, x, y, colour);
			c.restore();
		}

	}

	protected void onDraw(Canvas c) {
		mColour.setTextSize(25);

		mColour.setColor(getResources().getColor(R.color.cheery_pink));

		drawTextMaybeFlipped(
				c,
				"X: "
						+ formatString(Conversions
								.inchesToCm(PoseInfo.translationX)),
				calculateXDimension(3, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(3, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		drawTextMaybeFlipped(
				c,
				"Y: "
						+ formatString(Conversions
								.inchesToCm(PoseInfo.translationY)),
				calculateXDimension(5, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(5, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		drawTextMaybeFlipped(
				c,
				"Z: "
						+ formatString(Conversions
								.inchesToCm(PoseInfo.translationZ)),
				calculateXDimension(7, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(7, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		mColour.setColor(getResources().getColor(R.color.apple_yellow));

		drawTextMaybeFlipped(c, "R: " + formatString(PoseInfo.rotationRoll),
				calculateXDimension(9, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(9, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		drawTextMaybeFlipped(c, "P: " + formatString(PoseInfo.rotationPitch),
				calculateXDimension(11, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(11, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		drawTextMaybeFlipped(c, "Y: " + formatString(PoseInfo.rotationYaw),
				calculateXDimension(13, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				calculateYDimension(13, STATS_X_DIMENSION, DIST_NUMBERS_APART),
				mColour);

		mColour.setTextSize(70);

		mColour.setStyle(Paint.Style.STROKE);
		mColour.setColor(getResources().getColor(R.color.pacific_blue));
		drawTextMaybeFlipped(c, "Calibrating...",
				calculateXDimension(24, 100, DIST_NUMBERS_APART),
				calculateYDimension(24, 100, DIST_NUMBERS_APART), mColour);

		mColour.setTextSize(60);

		if (mCloser) {
			drawTextMaybeFlipped(c, "Closer",
					calculateXDimension(32, 120, DIST_NUMBERS_APART),
					calculateYDimension(32, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mFarther) {
			drawTextMaybeFlipped(c, "Farther",
					calculateXDimension(32, 120, DIST_NUMBERS_APART),
					calculateYDimension(32, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mRight) {
			drawTextMaybeFlipped(c, "Right",
					calculateXDimension(40, 120, DIST_NUMBERS_APART),
					calculateYDimension(40, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mLeft) {
			drawTextMaybeFlipped(c, "Left",
					calculateXDimension(40, 120, DIST_NUMBERS_APART),
					calculateYDimension(40, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mUp) {
			drawTextMaybeFlipped(c, "Up",
					calculateXDimension(48, 120, DIST_NUMBERS_APART),
					calculateYDimension(48, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mDown) {
			drawTextMaybeFlipped(c, "Down",
					calculateXDimension(48, 120, DIST_NUMBERS_APART),
					calculateYDimension(48, 120, DIST_NUMBERS_APART), mColour);
		}
		if (mNeedBetterLighting) {
			drawTextMaybeFlipped(c,
					"Darker Lighting Required (" + LightUtil.getLux() + ")",
					calculateXDimension(8, 100, DIST_NUMBERS_APART),
					calculateYDimension(8, 100, DIST_NUMBERS_APART), mColour);
		}
		if (mUnTiltHead) {
			drawTextMaybeFlipped(c,
					"Stop tilting your head",
					calculateXDimension(16, 100, DIST_NUMBERS_APART),
					calculateYDimension(16, 100, DIST_NUMBERS_APART), mColour);

		}

	}

	private String formatString(double value) {
		return String.format("%.2f", value);
	}

	public void startCalibrationDisplay() {
		Log.i(TAG, "calibration display started");
		stopped = false;
		mCalibrationDisplayHandler.postDelayed(mCalibrationLoop, mFrameRate);
	}

	public void stopCalibrationDisplay() {
		Log.i(TAG, "calibration display stopped");
		stopped = true;
		mCalibrationDisplayHandler.removeCallbacks(mCalibrationLoop);
	}

	private Runnable mCalibrationLoop = new Runnable() {
		@Override
		public void run() {

			if (!stopped) {
				// event is a head position problem
				if (!checkUserCalibration()) {

					mCountDownTime = -1;
					invalidate();
				} else {

					// tell main activity that the calibration event is
					// done so they redisplay the perimeter view

					listener.onCalibrationEvent(1);
					invalidate();

				}

			}
			// Delay the next update by FRAME_RATE milliseconds
			mCalibrationDisplayHandler
					.postDelayed(mCalibrationLoop, mFrameRate);

		}
	};

	// checks if user needs to be calibrated
	// looks at the coordinates and sees if they're close
	// calibration view checks and tells user if they should move left or right
	// or whatever
	public boolean checkUserCalibration() {
		boolean withinRange = true;
		mCloser = false;
		mFarther = false;
		mUp = false;
		mDown = false;
		mRight = false;
		mLeft = false;
		mNeedBetterLighting = false;
		mUnTiltHead = false;

		if (PoseInfo.deviceFlipped == 0) {
			if (PoseInfo.translationX > 4.4) {
				mLeft = true;
				withinRange = false;
			} else if (PoseInfo.translationX < 1.6) {
				mRight = true;
				withinRange = false;
			}
			if (PoseInfo.translationY > 3.2) {
				mUp = true;
				withinRange = false;
			} else if (PoseInfo.translationY < 0.5) {
				mDown = true;
				withinRange = false;

			}
		} else {
			if (PoseInfo.translationX > 4.4) {
				mUp = true;
				withinRange = false;
			} else if (PoseInfo.translationX < 1.6) {
				mDown = true;
				withinRange = false;
			}
			if (PoseInfo.translationY > 3.6) {
				mRight = true;
				withinRange = false;
			} else if (PoseInfo.translationY < 0) {
				mLeft = true;
				withinRange = false;
			}
		}

		if (PoseInfo.translationZ < 9.25) {
			mFarther = true;
			withinRange = false;
		} else if (PoseInfo.translationZ > 11) {
			mCloser = true;
			withinRange = false;
		}

		if (LightUtil.getLux() > 55) {
			mNeedBetterLighting = true;
			withinRange = false;
		}

		double yaw = PoseInfo.rotationYaw;

		if (Math.abs(yaw) > 20) {
			mUnTiltHead = true;
			withinRange = false;
		}

		return withinRange;
	}

	public CalibrationListener listener;

	public void setCalibrationDoneListener(CalibrationListener listener) {
		this.listener = listener;
	}

}
