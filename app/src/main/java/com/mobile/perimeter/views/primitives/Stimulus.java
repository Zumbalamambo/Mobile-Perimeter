package com.mobile.perimeter.views.primitives;

import android.util.Log;

import com.mobile.perimeter.light.LightUtil;
import com.mobile.perimeter.util.Consts;

public class Stimulus {
	private double mStimCoordX;
	private double mStimCoordY;
	private int mStimPosX;
	private int mStimPosY;
	private int mRadiusPixelsX;
	private int mRadiusPixelsY;
	private int mColour;
	private int mDiffSensitivity;
	private int mDone;
	private Consts.GoldmannSize mGoldmannSize;
	private int mNumMoves;
    private int mRedone;

	private boolean mIsDuplicate;

	private Consts.Stim mStimStatus;
	private Consts.Stim mInitStatus;
	private Consts.Stim mIntermediateStatus;

	public Stimulus(double x, double y) {
		mStimCoordX = x;
		mStimCoordY = y;
		mIsDuplicate = false;
		mGoldmannSize = Consts.GoldmannSize.III;
		mNumMoves = 0;
        mRedone = 0;
	}

	public Stimulus(double x, double y, boolean duplicate) {
		mStimCoordX = x;
		mStimCoordY = y;
		mIsDuplicate = duplicate;
		mGoldmannSize = Consts.GoldmannSize.III;
		mNumMoves = 0;
        mRedone = 0;
	}

	public void setPosition(int x, int y, int radiusPixelsX, int radiusPixelsY) {
		mStimPosX = x;
		mStimPosY = y;
		mRadiusPixelsX = radiusPixelsX;
		mRadiusPixelsY = radiusPixelsY;
		mDone = 0;
	}

	public void adjustStimulusIntensity(int pressed) {

		// recording how many moves it takes for a stimulus to find the
		// threshold
		mNumMoves++;

		int currDb = legalityDb(mDiffSensitivity);

		// a state machine that tracks the state of the stimulus (seen/missed
		// etc)
		switch (mStimStatus) {
		case MISSEDTWO:
			if (currDb == Consts.MAX_DB || currDb == Consts.MIN_DB) {
				setDone(1);
				return;
			}
			if (pressed > 0) {
				setStimStatus(Consts.Stim.SEEN);
			}
			break;
		case SEEN:
			if (pressed > 0) {
				setStimStatus(Consts.Stim.SEENTWO);
			} else {
				setStimStatus(Consts.Stim.MISSED);
			}
			break;
		case SEENTWO:
			if (currDb == Consts.MAX_DB || currDb == Consts.MIN_DB) {
				setDone(1);
				return;
			}
			if (pressed == 0) {
				setStimStatus(Consts.Stim.MISSED);
			}
			break;
		case MISSED:
			if (pressed > 0) {
				setStimStatus(Consts.Stim.SEEN);
			} else {
				setStimStatus(Consts.Stim.MISSEDTWO);
			}
			break;
		case NONE:
			if (pressed > 0) {
				setStimStatus(Consts.Stim.SEEN);
				setInitStatus(Consts.Stim.SEEN);
			} else {
				setStimStatus(Consts.Stim.MISSED);
				setInitStatus(Consts.Stim.MISSED);
			}
			setIntermediateStatus(Consts.Stim.NONE);
			break;
		}

		// 4:2 algorithm
		if (getInitStatus() == Consts.Stim.SEEN) {
			if (getIntermediateStatus() == Consts.Stim.NONE) {
				// decrease intensity until the user has missed twice
				if (getStimStatus() != Consts.Stim.MISSEDTWO) {
					// decrease stimulus intensity
					setDiffSensitivity(legalityDb(currDb + 4));
					Log.d("intensity", "HAS NOT MISSED TWICE ; decreasing -- "
							+ legalityDb(currDb + 4));
				} else {
					Log.d("intensity",
							"MISSED TWICE ; switching to increase -- "
									+ legalityDb(currDb - 2));
					setDiffSensitivity(legalityDb(currDb - 2));
					setIntermediateStatus(mStimStatus);
				}
			} else {
				// now increase intensity until user sees again
				if (getStimStatus() != Consts.Stim.SEEN) {
					// increase stimulus intensity
					Log.d("intensity", "MISSED ; increasing -- "
							+ legalityDb(currDb - 2));
					setDiffSensitivity(legalityDb(currDb - 2));
				} else {
					// set the current intensity as the threshold
					Log.d("intensity", "SEEN ; found threshold -- " + currDb);
					setDone(1);
					setDiffSensitivity(currDb);
				}
			}
		} else if (getInitStatus() == Consts.Stim.MISSED) {
			if (getIntermediateStatus() == Consts.Stim.NONE) {
				// increase intensity until the user has seen twice
				if (getStimStatus() != Consts.Stim.SEENTWO) {
					// increase stimulus intensity
					setDiffSensitivity(legalityDb(currDb - 4));
					Log.d("intensity",
							"HAS NOT BEEN SEEN TWICE ; increasing -- "
									+ legalityDb(currDb - 4));

				} else {
					setIntermediateStatus(mStimStatus);
					setDiffSensitivity(legalityDb(currDb + 2));
					Log.d("intensity", "SEEN TWICE ; switching to decrease -- "
							+ legalityDb(currDb + 2));

				}
			} else {
				// now decrease intensity until user misses
				if (getStimStatus() != Consts.Stim.MISSED) {
					// decrease stimulus intensity
					Log.d("intensity", "SEEN ; decreasing -- "
							+ legalityDb(currDb + 2));
					setDiffSensitivity(legalityDb(currDb + 2));
				} else {
					// set the current intensity + 2db as the threshold
					setDiffSensitivity(legalityDb(currDb - 2));
					setDone(1);
					Log.d("intensity", "MISSED ; found threshold -- "
							+ legalityDb(currDb - 2));

				}
			}
		}
	}

	protected int legalityDb(int db) {
		if (db < Consts.MAX_DB) {
			db = Consts.MAX_DB;
		}
		if (db > Consts.MIN_DB) {
			db = Consts.MIN_DB;
		}
		return db;
	}


	// Does error checking on colour obtained and changes dB if the colour is
	// too high
	// will want to change to some kind of while loop, or change it so that the
	// size can
	// only change to size V
	public void evalColourRequired() {
		mColour = LightUtil.calculateRGB(mStimPosX, mStimPosY, mDiffSensitivity);

        mGoldmannSize = Consts.GoldmannSize.III;
		if (mColour > 265) {
			mColour = LightUtil.calculateRGB(mStimPosX, mStimPosY, mDiffSensitivity + 5);
			mGoldmannSize = Consts.GoldmannSize.IV;
			if (mColour > 265) {
				mColour = LightUtil.calculateRGB(mStimPosX, mStimPosY, mDiffSensitivity + 10);
				mGoldmannSize = Consts.GoldmannSize.V;
			}
		}
		if (mColour > 255) {
			mColour = 255;
		} else if (mColour < 0) {
			mColour = 0;
		}

	}

	public void setStimStatus(Consts.Stim status) {
		mStimStatus = status;
	}

	public void setInitStatus(Consts.Stim status) {
		mInitStatus = status;
	}

	public void setIntermediateStatus(Consts.Stim status) {
		mIntermediateStatus = status;
	}

	public void setDone(int done) {
		mDone = done;
	}

	public void setDiffSensitivity(int diff) {
		mDiffSensitivity = diff;
	}

	public int getDiffSensitivity() {
		return mDiffSensitivity;
	}

	public double getStimCoordX() {
		return mStimCoordX;
	}

	public double getStimCoordY() {
		return mStimCoordY;
	}

	public int getStimPosX() {
		return mStimPosX;
	}

	public int getStimPosY() {
		return mStimPosY;
	}

	public int getRadiusPixelsX() {
		return mRadiusPixelsX;
	}
	
	public int getRadiusPixelsY() {
		return mRadiusPixelsY;
	}

	public int getNumMoves() {
		return mNumMoves;
	}

	public boolean getIsDuplicate() {
		return mIsDuplicate;
	}

	public Consts.GoldmannSize getGoldmannSize() {
		return mGoldmannSize;
	}

	public int getColour() {
		return mColour;
	}

	public Consts.Stim getStimStatus() {
		return mStimStatus;
	}

	public Consts.Stim getInitStatus() {
		return mInitStatus;
	}

	public Consts.Stim getIntermediateStatus() {
		return mIntermediateStatus;
	}

	public int getDone() {
		return mDone;
	}

    public void setRedone(int redone) { mRedone = redone; }

    public int getRedone() { return mRedone; }
}
