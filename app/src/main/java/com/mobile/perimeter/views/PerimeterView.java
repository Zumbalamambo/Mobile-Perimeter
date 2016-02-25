package com.mobile.perimeter.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mobile.perimeter.light.LightUtil;
import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.Debugging;
import com.mobile.perimeter.util.PoseInfo;
import com.mobile.perimeter.util.ScreenInfo;
import com.mobile.perimeter.views.primitives.CalibrationListener;
import com.mobile.perimeter.views.primitives.PerimetryDoneListener;
import com.mobile.perimeter.views.primitives.Quadrant;
import com.mobile.perimeter.views.primitives.Stimulus;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerimeterView extends View {

	protected static final String TAG = "PerimeterView";
	private Context mContext;
	// Frame Rate
	private int mFrameRate = 200;
	// Used to start the runnable again at _frameRate milliseconds
	private Handler mPerimetryHandler = new Handler();

	private boolean mPerimetryPaused;
	private boolean mPerimetryFlipped;
	private boolean mPerimetryDone;
	private boolean mUserNotReady;
	// if the test has just restarted, don't adjust stimulus intensity based on
	// previous stimulus
	private boolean mPerimetryRestarted;

	private Paint mStimulusColour;
	private Paint mBackgroundColour;

	private int mNumStimulusForCurLoc;
	private int mDisplayStimulus;
	private boolean mUpdateInProgress;
	private int mPressed;

	private ArrayList<Quadrant> mQuads = new ArrayList<Quadrant>();
	private Stimulus mCurrStimulus;

	private int mCurrQuad;
	private int mPrevQuad;

	public PerimeterView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	public PerimeterView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

	}

	public void startPerimetry() {

		mStimulusColour = new Paint();
		mBackgroundColour = new Paint();

		mNumStimulusForCurLoc = 0;
		mDisplayStimulus = 0;
		mPressed = 0;
		mPerimetryDone = false;
		mPerimetryFlipped = false;
		mPerimetryPaused = false;
		mUpdateInProgress = false;
		mUserNotReady = false;
		mCurrQuad = 0;

		if (!Consts.HEAD_TRACKER_MODE) {
			PoseInfo.setPoseInfoInches(PoseInfo.CONST_TRANSLATION_X_INCHES,
					PoseInfo.CONST_TRANSLATION_Y_INCHES,
					PoseInfo.CONST_TRANSLATION_Z_INCHES, 0,
					PoseInfo.rotationPitch, PoseInfo.rotationRoll);
					//PoseInfo.setPoseInfoCm(10.287, 4.7183, 25, 0, PoseInfo.rotationPitch,
					//PoseInfo.rotationRoll);
		}

		// mPerimetryHandler.postDelayed(_perimeterLoop, 200);

		mPerimetryPaused = true;
		mUserNotReady = true;
		mPressed = 0;
		invalidate();
	}

	public int get_frameRate() {
		return mFrameRate;
	}

	public void set_frameRate(int frame_rate) {
		mFrameRate = frame_rate;
	}

	protected void onUpdate() {

		// used to be an ||, changed b/c of restarting from mid-way, not fully.
		// Obtains new stimulus
		if (mQuads.get(mCurrQuad).getQuadDone() == 0 && !mUpdateInProgress) {

			// Time to display a new stimulus
			if (mDisplayStimulus == 0) {

				if (mCurrStimulus != null && !mPerimetryRestarted) {
					mCurrStimulus.adjustStimulusIntensity(mPressed);
				}
				// the restart was complete
				if (mPerimetryRestarted) {
					mPerimetryRestarted = false;
				}
				getNewStimulus();

			}
		}

		// Updating fixation location
		if (mQuads.get(mCurrQuad).getQuadDone() == 1 || mUpdateInProgress) {

			mUpdateInProgress = updateFixationLoc();
			if (mUpdateInProgress) {
				mNumStimulusForCurLoc++;
			} else {
				mPerimetryPaused = true;
				mUserNotReady = true;
				mNumStimulusForCurLoc = 0;
				mPressed = 0;
				invalidate();
			}
		}

		if (mPerimetryDone) {
			finishedPerimetry();
		}

	}

	protected void onDraw(Canvas c) {
		// TODO fix the hardcoded colour values of background

		if (!Consts.LIGHTING) {
			mBackgroundColour.setColor(Color.rgb(32, 32, 32));
		} else {
			mBackgroundColour.setColor(Color.rgb(37, 37, 37));
		}

		mBackgroundColour.setStyle(Style.FILL);
		c.drawPaint(mBackgroundColour);
		// Draw fixation point
		mStimulusColour.setColor(Color.rgb(255, 195, 77));
		c.drawCircle(mQuads.get(mCurrQuad).getFixationX(), mQuads
				.get(mCurrQuad).getFixationY(), mQuads.get(mCurrQuad)
				.getFixationR(), mStimulusColour);

		// Draw stimulus point or nothing, if waiting for user response
		if (!mUpdateInProgress && mCurrStimulus != null) {
			if (mDisplayStimulus == 0) {
				mPressed = 0;
				int colour = mCurrStimulus.getColour();
				mStimulusColour.setColor(Color.rgb(colour, colour, colour));
				Debugging.appendLog("Stimulus ("
						+ mCurrStimulus.getStimCoordX() + ","
						+ mCurrStimulus.getStimCoordY() + ") : "
						+ mCurrStimulus.getStimPosX() + ","
						+ mCurrStimulus.getStimPosY() + "Stimulus colour: "
						+ colour + "\ndb: "
						+ mCurrStimulus.getDiffSensitivity() + "\nr: "
						+ mCurrStimulus.getRadiusPixelsX() + " "
						+ mCurrStimulus.getRadiusPixelsY());

				Log.d("colour",
						"is " + colour + " db: "
								+ mCurrStimulus.getDiffSensitivity() + " r: "
								+ mCurrStimulus.getRadiusPixelsX()
								+ mCurrStimulus.getRadiusPixelsY());
				c.drawOval(
						new RectF(mCurrStimulus.getStimPosX()
								- mCurrStimulus.getRadiusPixelsX(),
								mCurrStimulus.getStimPosY()
										- mCurrStimulus.getRadiusPixelsY(),
								mCurrStimulus.getStimPosX()
										+ mCurrStimulus.getRadiusPixelsX(),
								mCurrStimulus.getStimPosY()
										+ mCurrStimulus.getRadiusPixelsY()),
						mStimulusColour);
				mDisplayStimulus++;
			} else if (mDisplayStimulus < Consts.TEST_SPEED_CONST) {
				mDisplayStimulus++;
			} else {
				mDisplayStimulus = 0;
			}
		}

		if (mUserNotReady) {
			mStimulusColour.setColor(Color.rgb(200, 200, 200));
			mStimulusColour.setTextSize(39);
			if (PoseInfo.deviceFlipped == 1) {
				c.save();
				c.rotate(-90, ScreenInfo.screenWidth / 2,
						ScreenInfo.screenHeight * 3 / 4);
				c.drawText("Click when you are fixated and ready",
						ScreenInfo.screenWidth / 2,
						ScreenInfo.screenHeight * 3 / 4, mStimulusColour);
				c.restore();
			} else {
				c.drawText("Click when you are fixated and ready",
						ScreenInfo.screenWidth / 3,
						ScreenInfo.screenHeight / 3, mStimulusColour);
			}
		}

	}

	private Runnable _perimeterLoop = new Runnable() {
		@Override
		public void run() {

			if (!mPerimetryPaused && !mPerimetryDone) {
				onUpdate(); // Update locations/speed
				// Delay the next update by FRAME_RATE milliseconds
				Random rn = new Random();
				int delay = rn.nextInt(200) - 100;
				if (mDisplayStimulus == 0) {
					delay = 0;
				}
				mPerimetryHandler.postDelayed(_perimeterLoop, mFrameRate
						+ delay);

				invalidate(); // Update visuals (ends up calling onDraw)
			}

		}
	};

	private boolean updateFixationLoc() {

		// keeps updating every frame until the fixation point has reached its
		// next destination

		if (mNumStimulusForCurLoc == 0) {
			mPrevQuad = mCurrQuad;
			// get the next quadrant if the current one is not the last one or
			// if the current is empty
			do {
				if (mCurrQuad < (mQuads.size() - 1)) {
					mCurrQuad++;
				} else {
					mPerimetryDone = true;
					return false;
				}
			} while (mQuads.get(mCurrQuad).getStimuli().size() == 0);
		}

		if (Consts.MODE == 2 && mCurrQuad == 5 && mPerimetryFlipped == false) {
			mPerimetryPaused = true;
			switchOrientationScreenDialog();

		}

		int _prevQuadX = mQuads.get(mPrevQuad).getFixationX();
		int _prevQuadY = mQuads.get(mPrevQuad).getFixationY();
		return mQuads.get(mCurrQuad).updateFixation(_prevQuadX, _prevQuadY,
				mNumStimulusForCurLoc);
		// once at the destination and properly updated, we now restart and
		// flash stimuli at this new location

	}

	protected void getNewStimulus() {
		if (mQuads.get(mCurrQuad).getQuadDone() == 0) {
			mCurrStimulus = mQuads.get(mCurrQuad).getStimulus();
		} else {
			mCurrStimulus = null;
			mUpdateInProgress = true;
		}

		if (mCurrStimulus != null) {
			mCurrStimulus.evalColourRequired();
			double yaw = PoseInfo.rotationYaw;

			if (Consts.HEAD_TRACKER_MODE
					&& Consts.CALIBRATION_MODE
					&& (mCurrStimulus.getStimPosX()
							+ mCurrStimulus.getRadiusPixelsX() > ScreenInfo.screenWidth
							|| mCurrStimulus.getStimPosX()
									- mCurrStimulus.getRadiusPixelsX() < 0
							|| mCurrStimulus.getStimPosY()
									+ mCurrStimulus.getRadiusPixelsY() > ScreenInfo.screenHeight
							|| mCurrStimulus.getStimPosY()
									- mCurrStimulus.getRadiusPixelsY() < 0
							|| !LightUtil.isAngleValid()
							|| LightUtil.getLux() > 55 || Math.abs(yaw) > 13)) {
				calibrationListener.onCalibrationEvent(0);
			}

		}
	}

	protected void switchOrientationScreenDialog() {
		AlertDialog.Builder flipScreenDialogBox = new AlertDialog.Builder(
				getContext());
		flipScreenDialogBox.setMessage("Please flip the screen clockwise.");
		flipScreenDialogBox.setCancelable(false);
		flipScreenDialogBox.setPositiveButton("Done",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						PoseInfo.deviceFlipped = 1;

						// no head tracking, therefore need to set where user
						// head is placed
						if (!Consts.HEAD_TRACKER_MODE) {
							PoseInfo.setPoseInfoInches(
									PoseInfo.CONST_TRANSLATION_X_INCHES,
									PoseInfo.CONST_TRANSLATION_Y_INCHES,
									PoseInfo.CONST_TRANSLATION_Z_INCHES, 90,
									PoseInfo.rotationPitch,
									PoseInfo.rotationRoll);
						}
						mPerimetryFlipped = true;
						restartPerimetry();
					}
				});

		AlertDialog alert11 = flipScreenDialogBox.create();
		alert11.show();
	}

	public void onCalibrationDone() {
		mPerimetryRestarted = true;

		mPerimetryPaused = true;
		mUserNotReady = true;
		mPressed = 0;
		invalidate();
	}

	public void restartPerimetry() {

		if (mPerimetryPaused) {
			mPerimetryPaused = false;
			mPerimetryHandler.removeCallbacks(_perimeterLoop);
			mPerimetryHandler.postDelayed(_perimeterLoop, mFrameRate * 15);
		}
		mPerimetryRestarted = true;
		mPressed = 0;

	}

	public void setPaused(boolean paused) {
		mPerimetryPaused = paused;
	}

	public void setInterrupted(boolean paused) {
		mPerimetryPaused = paused;
		setStopped(false, paused);
	}

	public void setStopped(boolean stopped, boolean paused) {
		mPerimetryDone = stopped;
		mPerimetryPaused = paused;
		mPerimetryHandler.removeCallbacks(_perimeterLoop);
		if (!mQuads.isEmpty() && listener != null) {
			listener.onPerimetryDone(mQuads, true);
		}
	}

	public boolean isPaused() {
		return mPerimetryPaused;
	}

	public void createQuads(int mode, boolean eye) {
		// mQuads = null;
		mQuads = new ArrayList<Quadrant>();

		int numQuads = 8;
		if (mode == 1 && !Consts.MODIFIED_DEMO) {
			numQuads = 1;
		} else if (mode == 1 && Consts.MODIFIED_DEMO) {
			numQuads = 2;
		} else if (mode == 2) {
			numQuads = 9;
		}

		for (int i = 0; i < numQuads; i++) {
			Quadrant temp = new Quadrant(i, eye);
			temp.setFixation(mode);
			mQuads.add(temp);
		}
	}

	public void createQuadsAndStimuli(String results, boolean loadedResults,
			boolean whichEye) {

		// if there is a loaded previous results
		if (loadedResults) {
			// Consts.LOADED_MODE = -1;
			return;
		}

		if (results != null) {
			createQuads(Consts.LOADED_MODE, whichEye);
			String[] stims = results.split(";");
			Pattern stimPattern = Pattern
					.compile("\\s*(\\(d\\))*\\s*quad (\\d+) \\((-?\\d+.\\d+),(-?\\d+.\\d+)\\): (-?\\d+)");

			int checkIfQuadDone = 1;
			int prevCheckIfQuadDone = 1;
			int prevQuad = -10;
			for (int i = 0; i < stims.length; i++) {
				Matcher m = stimPattern.matcher(stims[i]);
				if (m.matches()) {
					boolean isDuplicate = false;

					if (m.group(0).contains("(d)")) {
						isDuplicate = true;
					}
					int q = Integer.parseInt(m.group(2));
					double c1 = Double.parseDouble(m.group(3));
					double c2 = Double.parseDouble(m.group(4));
					int r = Integer.parseInt(m.group(5));
					Stimulus s = new Stimulus(c1, c2, isDuplicate);
					// if sensitivity isn't -1, it's valid and record it into
					// the stimulus so we don't have to redo it
					if (r != -1) {
						s.setDiffSensitivity(r);
						s.setDone(1);
					} else {
						prevCheckIfQuadDone = checkIfQuadDone;
						checkIfQuadDone = 0;
					}

					mQuads.get(q).insertIntoQuadrant(s);
					// if we've now finished entering the previous quadrant's or
					// this is the last one, check if the checkdone means the
					// quad is done
					if ((prevQuad + 1) == q && prevCheckIfQuadDone == 1) {
						mQuads.get(prevQuad).setQuadDone(1);
					}
					if ((i + 1) == stims.length && checkIfQuadDone == 1) {
						mQuads.get(q).setQuadDone(1);
					}
					prevQuad = q;
				}
			}
			return;
		}

		createQuads(Consts.MODE, whichEye);
		// Blind Spot Test
		// 9, 11, 13, 15, 17, 19, 21 degrees horizontally, -3 vertically
		if (Consts.MODE == 1 && !Consts.MODIFIED_DEMO) {
			mQuads.get(0).insertIntoQuadrant(new Stimulus(2, -1));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus((double) 7.0 / 3.0, -1));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus((double) 8.0 / 3.0, -1));
			mQuads.get(0).insertIntoQuadrant(new Stimulus(3, -1));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus((double) 10.0 / 3.0, -1));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus((double) 11.0 / 3.0, -1));
			mQuads.get(0).insertIntoQuadrant(new Stimulus(4, -1));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus(3, (double) 2.0 / 3.0));
			mQuads.get(0).insertIntoQuadrant(
					new Stimulus(3, (double) -2.0 / 3.0));
		} else if (Consts.MODE == 1 && Consts.MODIFIED_DEMO) {
			mQuads.get(0).insertIntoQuadrant(new Stimulus(2, -1));
			mQuads.get(0).insertIntoQuadrant(new Stimulus(3, -1));
			mQuads.get(0).insertIntoQuadrant(new Stimulus(-2, -1));
			mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, -2));
			mQuads.get(1).insertIntoQuadrant(new Stimulus(3, 1));
			mQuads.get(1).insertIntoQuadrant(new Stimulus(1, 1));
			mQuads.get(1).insertIntoQuadrant(new Stimulus(3, 3));
			mQuads.get(1).insertIntoQuadrant(new Stimulus(1, 3));
			// Center Fixation Method
		} else if (Consts.MODE == 2) {

			// Abbreviated test for Center Fixation Method
			if (!Consts.FULL_TEST) {
				mQuads.get(0).insertIntoQuadrant(new Stimulus(1, 1));
				// mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, -1));
				// mQuads.get(0).insertIntoQuadrant(new Stimulus(3, -1));
				// mQuads.get(0).insertIntoQuadrant(new Stimulus(-2, 2));

				mQuads.get(1).insertIntoQuadrant(new Stimulus(3, 1));
				// mQuads.get(1).insertIntoQuadrant(new Stimulus(4, 2));
				// mQuads.get(1).insertIntoQuadrant(new Stimulus(1, 3));

				mQuads.get(2).insertIntoQuadrant(new Stimulus(4, -2));
				// mQuads.get(2).insertIntoQuadrant(new Stimulus(3, -3));
				// mQuads.get(2).insertIntoQuadrant(new Stimulus(1, -3));

				mQuads.get(3).insertIntoQuadrant(new Stimulus(-3, -1));
				// mQuads.get(3).insertIntoQuadrant(new Stimulus(-4, -2));
				// mQuads.get(3).insertIntoQuadrant(new Stimulus(-1, -3));
				// mQuads.get(3).insertIntoQuadrant(new Stimulus(-5, -1));

				mQuads.get(4).insertIntoQuadrant(new Stimulus(-4, 2));
				// mQuads.get(4).insertIntoQuadrant(new Stimulus(-3, 3));
				// mQuads.get(4).insertIntoQuadrant(new Stimulus(-1, 3));
				// mQuads.get(4).insertIntoQuadrant(new Stimulus(-5, 1));

				mQuads.get(7).insertIntoQuadrant(new Stimulus(2, 4));

				mQuads.get(8).insertIntoQuadrant(new Stimulus(2, -4));

				mQuads.get(5).insertIntoQuadrant(new Stimulus(-2, -4));

				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 4));

			} else {
				// Full test for Center Fixation Method

				mQuads.get(0).insertIntoQuadrant(new Stimulus(-2, -1));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(1, -1));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, -2));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(3, -1, true));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, -1));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, 1));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(-1, 2));
				mQuads.get(0).insertIntoQuadrant(new Stimulus(2, 1));

				// 10 Double determination points
				mQuads.get(0).insertIntoQuadrant(new Stimulus(1, -1, true));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(4, 1, true));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(3, -3, true));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-5, -1, true));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-3, 1, true));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(1, 4, true));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(2, -4, true));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-1, -4, true));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-2, -2, true));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 3, true));

				mQuads.get(1).insertIntoQuadrant(new Stimulus(1, 1));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(3, 1));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(4, 1));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(4, 2));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(3, 3));
				mQuads.get(1).insertIntoQuadrant(new Stimulus(1, 3));

				mQuads.get(2).insertIntoQuadrant(new Stimulus(2, -1));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(3, -1));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(4, -1));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(1, -3));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(4, -2));
				mQuads.get(2).insertIntoQuadrant(new Stimulus(3, -3));

				mQuads.get(3).insertIntoQuadrant(new Stimulus(-3, -1));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-4, -1));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-4, -2));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-5, -1));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-3, -3));
				mQuads.get(3).insertIntoQuadrant(new Stimulus(-1, -3));

				mQuads.get(4).insertIntoQuadrant(new Stimulus(-3, 1));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-4, 1));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-4, 2));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-5, 1));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-3, 3));
				mQuads.get(4).insertIntoQuadrant(new Stimulus(-1, 3));

				mQuads.get(7).insertIntoQuadrant(new Stimulus(1, 4));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(2, 4));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(2, 2));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(2, 3));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(3, 2));
				mQuads.get(7).insertIntoQuadrant(new Stimulus(1, 2));

				mQuads.get(8).insertIntoQuadrant(new Stimulus(1, -2));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(1, -4));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(2, -4));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(2, -3));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(3, -2));
				mQuads.get(8).insertIntoQuadrant(new Stimulus(2, -2));

				mQuads.get(5).insertIntoQuadrant(new Stimulus(-1, -4));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-2, -4));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-2, -3));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-3, -2));
				mQuads.get(5).insertIntoQuadrant(new Stimulus(-2, -2));

				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 1));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-1, 4));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 4));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 3));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-3, 2));
				mQuads.get(6).insertIntoQuadrant(new Stimulus(-2, 2));

			}
		}
	}

	public ArrayList<Quadrant> getQuads() {
		return mQuads;
	}

	protected void finishedPerimetry() {
		this.setStopped(true, false);
	}

	public boolean getPerimetryDone() {
		return mPerimetryDone;
	}

	public PerimetryDoneListener listener;

	public void setPerimetryDoneListener(PerimetryDoneListener listener) {
		this.listener = listener;
	}

	public CalibrationListener calibrationListener;

	public void setCalibrationDoneListener(CalibrationListener listener) {
		this.calibrationListener = listener;
	}

	public void setPressed() {
		mPressed = 1;
		if (mUserNotReady && mPerimetryPaused) {
			mUserNotReady = false;
			mPressed = 0;
			invalidate();
			restartPerimetry();
		}
	}

}
