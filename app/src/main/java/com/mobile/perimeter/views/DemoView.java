package com.mobile.perimeter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mobile.perimeter.R;
import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.ScreenInfo;
import com.mobile.perimeter.views.primitives.Quadrant;
import com.mobile.perimeter.views.primitives.Stimulus;

public class DemoView extends View {
	protected static final String TAG = "DemoView";

	private Quadrant demoQuad;
	private Paint mColour = new Paint();
	private Handler mDemoDisplayHandler = new Handler();
	private int mFrameRate = 200;
	private boolean mNewPoseAvailable = false;

	public DemoView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub

	}

	public DemoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onDraw(Canvas c) {

		mColour.setColor(getResources().getColor(R.color.pacific_blue));
		mColour.setStyle(Style.FILL);
		c.drawCircle(demoQuad.getFixationX(), demoQuad.getFixationY(),
				demoQuad.getFixationR(), mColour);

		mColour.setColor(getResources().getColor(R.color.dusky_pink));

		Stimulus s;
		for (int i = 0; i < demoQuad.getNumStimuli(); i++) {
			s = demoQuad.getStimulus(i);
			
			c.drawOval(
					new RectF(s.getStimPosX() - s.getRadiusPixelsX(), s
							.getStimPosY() - s.getRadiusPixelsY(), s
							.getStimPosX() + s.getRadiusPixelsX(), s
							.getStimPosY() + s.getRadiusPixelsY()), mColour);
		}

	}

	public void startDemoDisplay() {
		Log.i(TAG, "start demo display");

		demoQuad = new Quadrant(0, Consts.RIGHT_EYE);
		Stimulus s;

		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				if (i != 0 && j != 0) {
					s = new Stimulus(i, j);
					demoQuad.insertIntoQuadrant(s);
				}
			}
		}

		demoQuad.setFixationX(ScreenInfo.screenWidth / 2);
		demoQuad.setFixationY(ScreenInfo.screenHeight / 2);
		demoQuad.setFixationR(22);
		mDemoDisplayHandler.postDelayed(mDemoLoop, mFrameRate);

	}

	public void stopDemoDisplay() {
		mDemoDisplayHandler.removeCallbacks(mDemoLoop);
	}

	private Runnable mDemoLoop = new Runnable() {
		@Override
		public void run() {

			if (mNewPoseAvailable) {
				mNewPoseAvailable = false;
				invalidate(); // Update visuals (ends up calling onDraw)
			}

			// Delay the next update by FRAME_RATE milliseconds
			mDemoDisplayHandler.postDelayed(mDemoLoop, mFrameRate);

		}
	};

	public void refreshDemoView() {
		mNewPoseAvailable = true;
	}
}
