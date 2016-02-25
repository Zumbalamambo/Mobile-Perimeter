package com.mobile.perimeter.views.primitives;

import android.util.Log;

import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.Conversions;
import com.mobile.perimeter.util.PoseInfo;
import com.mobile.perimeter.util.ScreenInfo;

import java.util.ArrayList;
import java.util.Random;

public class Quadrant {

	// List of stimuli in quadrant
	private ArrayList<Stimulus> mStimuliQuad;

	private static double STATIC_COUNTERROLL_CONST = 0.9;
	private static int POST_FIXATION_WAIT = 30;
	// Quadrant id
	private int mQuadId;

	// Fixation point locations as the quadrant changes
	private int mFixationX;
	private int mFixationY;
	private int mFixationR;

	// Fixation point for this quadrant - will never change
	private int mConstFixationX;
	private int mConstFixationY;

	// How much the fixation point will travel every frame
	private int mTravelDistX;
	private int mTravelDistY;

	private static int FIXATION_CONST = 100;

	// When all thresholds have been found for this quadrant
	private int mQuadDone = 0;

	private int mNumStimuliToDisplay;

	private int mNumMovesToWait = 3;
	
	private boolean mRightEye;

	public Quadrant(int id, boolean whichEye) {
		mQuadId = id;
		mRightEye = whichEye;
		mStimuliQuad = new ArrayList<Stimulus>();
	}

	public void insertIntoQuadrant(Stimulus s) {
		if (s.getDone() == 0) {

			s.setStimStatus(Consts.Stim.NONE);
			s.setDiffSensitivity(Consts.startingDb);
			s.evalColourRequired();

			mStimuliQuad.add(s);

		} else {
			mStimuliQuad.add(s);

		}
		mNumStimuliToDisplay = (mStimuliQuad.size() * 2) / 3;
		if (mNumStimuliToDisplay <= 0) {
			mNumStimuliToDisplay = mStimuliQuad.size();
		}
	}

	// checks to see if any of the stimuli need to be redone
	private void checkForRedo() {
		int mean = 0, count = 0;

		// finds the mean of all stimuli in a quadrant, excluding blind spot
		// areas
		for (int i = 0; i < mStimuliQuad.size(); i++) {
			Stimulus s = mStimuliQuad.get(i);
			if (s.getDone() == 1 && s.getStimCoordX() != 3
					&& (s.getStimCoordY() != 1 && s.getStimCoordY() != -1)) {
				mean += s.getDiffSensitivity();
				count++;
			}
		}

		if (count > 0) {
			mean = mean / count;
		}

		// checks to see if any of them are around 6db out of range of the mean,
		// reset these unless it's already been reset before
		/*for (int i = 0; i < mStimuliQuad.size(); i++) {
			Stimulus s = mStimuliQuad.get(i);
			if (s.getDone() == 1 && s.getStimCoordX() != 3
					&& (s.getStimCoordY() != 1 && s.getStimCoordY() != -1)) {
				if (Math.abs(mean - s.getDiffSensitivity()) > 6
						&& s.getRedone() != 1) {
					s.setDone(0);
					s.setRedone(1);
					s.setDiffSensitivity(24);
					s.setStimStatus(Consts.Stim.NONE);
				}
			}
		}*/
	}

	public Stimulus getStimulus() {
		Random rand = new Random();
		int max = mStimuliQuad.size();

		if (mStimuliQuad.size() == 0) {
			mQuadDone = 1;
			return null;
		}

		int r = rand.nextInt(mNumStimuliToDisplay);
		Stimulus s = mStimuliQuad.get(r);

		if (s.getNumMoves() >= mNumMovesToWait && mNumStimuliToDisplay < max) {
			mNumStimuliToDisplay++;
		}
		int count = 0;
		while (s.getDone() == 1 && count < max) {

			checkForRedo();
			mNumMovesToWait = s.getNumMoves() / 3;
			if (r < max - 1) {
				r++;
			} else {
				r = 0;
			}
			count++;
			s = mStimuliQuad.get(r);
		}

		// loop was exited because it couldn't find a stimulus that wasn't done,
		// set quadrant to done
		if (count == max) {
			mQuadDone = 1;
			return null;
		}

		double sX = s.getStimCoordX();
		double sY = s.getStimCoordY();

		double angleX = toAngle(sX);
		double angleY = toAngle(sY);

		/*
		 * if (Consts.MODE == 2) { switch (mQuadId) { case 0: case 1: case 2:
		 * case 3: case 4: break; case 5: case 6: case 7: case 8:
		 */
		
		if (!mRightEye) {
			// posX = flipPositionForLeftEye(posX);
			angleX = -angleX;
		}

		if (PoseInfo.deviceFlipped == 1) {
			double temp = angleX;
			angleX = -angleY;
			angleY = temp;
		}

		/*
		 * break; default: break; } }
		 */

		return calculateStimulusPosition(s, sX, sY, angleX, angleY);

	}

	public Stimulus getStimulus(int stimNum) {

		if (mStimuliQuad.size() == 0) {
			return null;
		}

		Stimulus s = mStimuliQuad.get(stimNum);

		double sX = s.getStimCoordX();
		double sY = s.getStimCoordY();

		double angleX = toAngle(sX);
		double angleY = toAngle(sY);

		if (!mRightEye) {
			// posX = flipPositionForLeftEye(posX);
			angleX = -angleX;
		}
		if (PoseInfo.deviceFlipped == 1) {
			double temp = angleX;
			angleX = -angleY;
			angleY = temp;
		}

		return calculateStimulusPosition(s, sX, sY, angleX, angleY);
	}

	private Stimulus calculateStimulusPosition(Stimulus s, double sX,
			double sY, double angleX, double angleY) {
		int posXint, posYint, posX, posY;

		double tX, tY, tZ, yaw;
		// if (Consts.HEAD_TRACKER_MODE == 1) {
		tX = PoseInfo.translationX;
		tY = PoseInfo.translationY;
		tZ = PoseInfo.translationZ;
		yaw = PoseInfo.rotationYaw;

		if (!Consts.HEAD_TRACKER_MODE || !Consts.USE_ESTIMATOR_MODE) {
			tX = PoseInfo.CONST_TRANSLATION_X_INCHES;
			tY = PoseInfo.CONST_TRANSLATION_Y_INCHES;
			tZ = PoseInfo.CONST_TRANSLATION_Z_INCHES;
			yaw = 0;
		}

		posXint = getPositionStimulus(
				Conversions.toPixels(tZ, ScreenInfo.xDPI),
				Conversions.toPixels(tX, ScreenInfo.xDPI), mFixationX, angleX,
				0);
		posYint = getPositionStimulus(
				Conversions.toPixels(tZ, ScreenInfo.yDPI),
				Conversions.toPixels(tY, ScreenInfo.yDPI), mFixationY, angleY,
				1);

		// rotate the x and y positions according to the yaw
		int posXRotated = rotationAfterYaw(posXint, posYint, yaw, 0);
		int posYRotated = rotationAfterYaw(posXint, posYint, yaw, 1);

		posX = posXRotated + mFixationX;
		posY = -posYRotated + mFixationY;

		Log.d("quadrant", "AFTER addition final pos: " + sX + "," + sY
				+ " ; posX = " + posX + " ; posY = " + posY);

		int radiusPixelsX = getRadiusFromGoldmann(s.getGoldmannSize().val(),
				angleX, ScreenInfo.xDPI,
				Conversions.toPixels(tZ, ScreenInfo.xDPI),
				Conversions.toPixels(tX, ScreenInfo.xDPI), mFixationX, 0);
		int radiusPixelsY = getRadiusFromGoldmann(s.getGoldmannSize().val(),
				angleY, ScreenInfo.yDPI,
				Conversions.toPixels(tZ, ScreenInfo.yDPI),
				Conversions.toPixels(tY, ScreenInfo.yDPI), mFixationY, 1);

		s.setPosition(posX, posY, radiusPixelsX, radiusPixelsY);

		return s;
	}

	protected int rotationAfterYaw(int x, int y, double yaw, int xOrY) {
		double yawRad = Conversions.toRadians(yaw * STATIC_COUNTERROLL_CONST);
		int rotated;
		if (xOrY == 0) {
			rotated = (int) ((int) (x) * (Math.cos(yawRad)) + (y)
					* (Math.sin(yawRad)));
		} else {
			rotated = (int) ((int) -(x) * (Math.sin(yawRad)) + (y)
					* (Math.cos(yawRad)));
		}
		return rotated;
	}

	protected int getPositionStimulus(double headTranslationZ,
			double headTranslationXY, double fixationPosition,
			double angleBeingTested, int xOrY) {
		double fixationPointChange = fixationPosition - headTranslationXY;

		// if we're doing Y values, then since the coordinate system is flipped
		// so vertically up is lower numbers (so the opposite)
		// then we have to change the fixationpointchange to the negative value
		// so it reflects a proper x-y axis system
		if (xOrY == 1) {
			fixationPointChange = -fixationPointChange;
		}
		double temp = (headTranslationZ
				* (Math.tan(Math.atan(fixationPointChange / headTranslationZ)
						+ Conversions.toRadians(angleBeingTested))) - fixationPointChange);

		return (int) temp;
	}

	protected int getRadiusFromGoldmann(double goldmann, double angle,
			double dpi, double headTranslationZ, double headTranslationXY,
			double fixationPosition, int xOrY) {
		double radiusRad = Conversions.toRadians(goldmann / 2);
		double angleRad = Conversions.toRadians(angle);
		double revisedRadius = Math.tan(angleRad)
				- Math.tan(angleRad - radiusRad);

		int totalAngle = getPositionStimulus(headTranslationZ,
				headTranslationXY, fixationPosition, angle + (goldmann / 2),
				xOrY);
		int minusGoldmannAngle = getPositionStimulus(headTranslationZ,
				headTranslationXY, fixationPosition, angle - (goldmann / 2),
				xOrY);

		return (totalAngle - minusGoldmannAngle) / 2;
		// return (int) (toPixels(revisedRadius * translationZ, dpi) + 0.5);

	}

	// protected void
	public void setFixation(int mode) {
		if (mode == 1 && !Consts.MODIFIED_DEMO) {
			mFixationX = 100;
			mFixationY = ScreenInfo.screenHeight / 2;
		} else if (mode == 1 && Consts.MODIFIED_DEMO) {
			switch (mQuadId) {
			case 0:
				mFixationX = (ScreenInfo.screenWidth * 4) / 9;
				mFixationY = ScreenInfo.screenHeight / 2;
				break;
			case 1:
				mFixationX = FIXATION_CONST;
				mFixationY = ScreenInfo.screenHeight - FIXATION_CONST;
				break;
			}
		} else if (mode == 2) {
			switch (mQuadId) {
			case 0:
				mFixationX = (ScreenInfo.screenWidth * 4) / 9;
				mFixationY = ScreenInfo.screenHeight / 2;
				break;
			case 1:
			case 8:
				mFixationX = FIXATION_CONST;
				mFixationY = ScreenInfo.screenHeight - FIXATION_CONST;
				break;
			case 2:
			case 5:
				mFixationX = FIXATION_CONST;
				mFixationY = FIXATION_CONST;
				break;
			case 3:
			case 6:
				mFixationX = ScreenInfo.screenWidth - FIXATION_CONST;
				mFixationY = FIXATION_CONST;
				break;
			case 4:
			case 7:
				mFixationX = ScreenInfo.screenWidth - FIXATION_CONST;
				mFixationY = ScreenInfo.screenHeight - FIXATION_CONST;
				break;
			}
		}

		if (!mRightEye) {
			flipPositionForLeftEye();
			
		}

		mConstFixationX = mFixationX;
		mConstFixationY = mFixationY;

		mFixationR = getRadiusFromGoldmann(Consts.GoldmannSize.III.val(),
				Consts.GoldmannSize.III.val() / 2, ScreenInfo.xDPI,
				Conversions.toPixels(PoseInfo.translationZ, ScreenInfo.xDPI),
				Conversions.toPixels(PoseInfo.translationX, ScreenInfo.xDPI),
				mFixationX, 0);

	}

	private void flipPositionForLeftEye() {
		switch (mQuadId) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
			mFixationX = ScreenInfo.screenWidth - mFixationX;
			break;
		case 5:
		case 6:
		case 7:
		case 8:
			mFixationY = ScreenInfo.screenHeight - mFixationY;
			break;

		}
	}

	public boolean updateFixation(int prevQuadX, int prevQuadY, int count) {

		if (mFixationX == mConstFixationX && mFixationY == mConstFixationY
				&& count != 0) {
			if (count < POST_FIXATION_WAIT) {
				return true;
			} else {
				return false;
			}
		} else {
			if (count == 0) {
				if (mFixationY != prevQuadY) {
					mTravelDistY = (mFixationY - prevQuadY) / 20;
				} else {
					mTravelDistY = 0;
				}
				if (mFixationX != prevQuadX) {
					mTravelDistX = (int) ((double) (mFixationX - prevQuadX)) / 20;
				} else {
					mTravelDistX = 0;
				}
				mFixationX = prevQuadX + mTravelDistX;
				mFixationY = prevQuadY + mTravelDistY;
			} else {
				mFixationX += mTravelDistX;
				mFixationY += mTravelDistY;
			}
		}
		if (mFixationX >= (mConstFixationX - 15)
				&& mFixationX <= (mConstFixationX + 15)
				&& mFixationY >= (mConstFixationY - 15)
				&& mFixationY <= (mConstFixationY + 15)) {
			mFixationX = mConstFixationX;
			mFixationY = mConstFixationY;
		}
		return true;
	}

	public int getFixationX() {
		return mFixationX;
	}

	public int getFixationY() {
		return mFixationY;
	}

	public int getQuadDone() {
		return mQuadDone;
	}

	public int getFixationR() {
		return mFixationR;
	}

	public int getQuadId() {
		return mQuadId;
	}

	public int getNumStimuli() {
		return mStimuliQuad.size();
	}

	public ArrayList<Stimulus> getStimuli() {
		return mStimuliQuad;
	}
	
	public boolean getEye() {
		return mRightEye;
	}

	public void setFixationX(int x) {
		mFixationX = x;
	}

	public void setFixationY(int y) {
		mFixationY = y;
	}

	public void setQuadDone(int done) {
		mQuadDone = done;
	}

	public void setFixationR(int r) {
		mFixationR = r;
	}

	protected double toAngle(double coord) {
		if (coord > 0) {
			return coord * 6 - 3;
		} else if (coord == 0) {
			return 0;
		} else {
			return coord * 6 + 3;
		}
	}

}
