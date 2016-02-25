package com.mobile.perimeter.poseestimation;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.Debugging;
import com.mobile.perimeter.util.PoseInfo;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class PoseEstimator {

	private static final String TAG = "PoseEstimator";

	private OnPoseEstimationDoneListener mListener;

	private boolean mPatternWasFound = false;
	private boolean mProcessFrameDone = false;
	private MatOfPoint2f mCorners = new MatOfPoint2f();
	private boolean mIsCalibrated = false;

	private Mat mCameraMatrix = new Mat();
	private Mat mDistortionCoefficients = new Mat();
	private Mat mRvec = new Mat();
	private Mat mTvec = new Mat();
	private Mat mEyePosition = new Mat();
	private int mFlags;
	private double mRms;
	private Size mImageSize;
	private double mRoll;
	private double mPitch;
	private double mYaw;

	private int mNumTimesNoPatternFound = 0;

	private Mat mPositions = new Mat();

	public native void FindChessboard(long matAddrRgba, long matAddrPtvec,
			int boardHeight, int boardWidth);

	public PoseEstimator(int width, int height) {
		mImageSize = new Size(width, height);
		mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT
				+ Calib3d.CALIB_ZERO_TANGENT_DIST
				+ Calib3d.CALIB_FIX_ASPECT_RATIO + Calib3d.CALIB_FIX_K4
				+ Calib3d.CALIB_FIX_K5;
		Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
		mCameraMatrix.put(0, 0, 1.0);

		Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public Mat rgbaFrame;

	private class processingFrame extends AsyncTask<Mat, Void, Void> {

		@Override
		protected Void doInBackground(Mat... frame) {
			Mat rgbaFrame = frame[0];

			mProcessFrameDone = false;
			Log.i(TAG, "cam matrix: " + mCameraMatrix.dump());
			Log.i(TAG, "dis matrix: " + mDistortionCoefficients.dump());

			boolean foundPattern = findPattern(rgbaFrame);
			Log.i(TAG, "pattern: " + mPatternWasFound);

			if (foundPattern) {
				findPose(rgbaFrame);
				mNumTimesNoPatternFound = 0;
			} else {
				mNumTimesNoPatternFound++;
				if (mNumTimesNoPatternFound > 30) {
					mNumTimesNoPatternFound = 0;
					PoseInfo.setPoseInfoCm(0, 0, 0, PoseInfo.rotationYaw,
							PoseInfo.rotationPitch, PoseInfo.rotationRoll);
				}
			}
			mListener.onProcessFrameDone();

			mProcessFrameDone = true;

			return null;
		}

	}

	private class proFrame implements Runnable {

		Mat rgbaFrame = new Mat();

		proFrame(Mat frame) {
			rgbaFrame = frame;
		}

		@Override
		public void run() {
			mProcessFrameDone = false;
			// Log.i(TAG, "cam matrix: " + mCameraMatrix.dump());
			// Log.i(TAG, "dis matrix: " + mDistortionCoefficients.dump());

			boolean foundPattern = findPattern(rgbaFrame);
			Log.i(TAG, "pattern: " + mPatternWasFound);

			if (foundPattern) {
				findPose(rgbaFrame);
				mNumTimesNoPatternFound = 0;
			} else {
				mNumTimesNoPatternFound++;
				if (mNumTimesNoPatternFound > 10) {
					mNumTimesNoPatternFound = 0;
					PoseInfo.setPoseInfoCm(0, 0, 0, PoseInfo.rotationYaw,
							PoseInfo.rotationPitch, PoseInfo.rotationRoll);
				}
			}
			mListener.onProcessFrameDone();

			mProcessFrameDone = true;
		}

	}

	public void processFrame(Mat rgbaFrame) {
		Log.i(TAG, "Started processing frame inside pose estimator");
		// new processingFrame().execute(rgbaFrame);
		new proFrame(rgbaFrame).run();

	}

	private boolean findPattern(Mat grayFrame) {
		mPatternWasFound = false;
		FindChessboard(grayFrame.getNativeObjAddr(),
				mCorners.getNativeObjAddr(), Consts.BOARD_HEIGHT,
				Consts.BOARD_WIDTH);

		if (mCorners.size().height == Consts.BOARD_HEIGHT * Consts.BOARD_WIDTH) {
			mPatternWasFound = true;
		}

		return mPatternWasFound;
	}

	private void findPose(Mat grayFrame) {
		MatOfPoint3f objectPoints = new MatOfPoint3f();
		generate3DCoordinates(objectPoints);

		// Log.i(TAG, "objPoints= " + objectPoints.dump());
		// Log.i(TAG, "mCorners= " + mCorners.dump());

		Calib3d.solvePnP(objectPoints, mCorners, mCameraMatrix,
				new MatOfDouble(mDistortionCoefficients), mRvec, mTvec);

		Mat R = new Mat();
		Calib3d.Rodrigues(mRvec, R);

		printPosition(R, mTvec);

		mEyePosition = calculationPosition(R, mTvec, PoseInfo.eyeCoordY,
				PoseInfo.eyeCoordX, PoseInfo.eyeCoordZ);
		calculateRotation(R);
		PoseInfo.setPoseInfoCm(-mEyePosition.get(0, 0)[0],
				mEyePosition.get(1, 0)[0] + 2.2, mEyePosition.get(2, 0)[0],
				mYaw, mPitch, mRoll);

		projectBackOntoImage(grayFrame);
	}

	private void projectBackOntoImage(Mat grayFrame) {
		MatOfPoint3f objectPoints = new MatOfPoint3f();
		objectPoints.push_back(new MatOfPoint3f(new Point3(3, 0, 0)));
		objectPoints.push_back(new MatOfPoint3f(new Point3(0, 3, 0)));
		objectPoints.push_back(new MatOfPoint3f(new Point3(0, 0, -3)));
		objectPoints.push_back(new MatOfPoint3f(new Point3(0, 0, 0)));

		// Log.i(TAG, "axes obj pts: " + objectPoints.dump());
		MatOfPoint2f imagePoints = new MatOfPoint2f();
		Calib3d.projectPoints(objectPoints, mRvec, mTvec, mCameraMatrix,
				new MatOfDouble(mDistortionCoefficients), imagePoints);

		drawPoints(grayFrame);

		drawAxes(grayFrame, imagePoints);

		drawEyeLocation(grayFrame);
	}

	private void drawEyeLocation(Mat grayFrame) {
		MatOfPoint3f objectPoints = new MatOfPoint3f();
		objectPoints.push_back(new MatOfPoint3f(new Point3(PoseInfo.eyeCoordX,
				PoseInfo.eyeCoordY, PoseInfo.eyeCoordZ)));

		// Log.i(TAG, "eye obj pts: " + objectPoints.dump());

		MatOfPoint2f imagePoints = new MatOfPoint2f();

		Calib3d.projectPoints(objectPoints, mRvec, mTvec, mCameraMatrix,
				new MatOfDouble(mDistortionCoefficients), imagePoints);

		Point center = getPointFromMat(imagePoints, 0);
		Imgproc.circle(grayFrame, center, 13, new Scalar(255, 0, 255), 3);
	}

	private void drawAxes(Mat grayFrame, MatOfPoint2f imagePoints) {

		Point p = getPointFromMat(imagePoints, 3);
		Point ix = getPointFromMat(imagePoints, 0);
		Point iy = getPointFromMat(imagePoints, 1);
		Point iz = getPointFromMat(imagePoints, 2);

		Imgproc.line(grayFrame, p, ix, new Scalar(0, 255, 255), 5);
		Imgproc.line(grayFrame, p, iy, new Scalar(0, 255, 255), 5);
		Imgproc.line(grayFrame, p, iz, new Scalar(0, 255, 255), 5);

	}

	private Point getPointFromMat(Mat matrix, int row) {
		Point p = new Point();

		p.x = matrix.get(row, 0)[0];
		p.y = matrix.get(row, 0)[1];
		return p;
	}

	private Mat calculationPosition(Mat r, Mat tvec, double row, double col,
			double depth) {
		Mat pvec = new Mat(3, 1, CvType.CV_64F);
		pvec.put(0, 0, col);
		pvec.put(1, 0, row);
		pvec.put(2, 0, depth);
		Mat pos = new Mat();

		Core.gemm(r, pvec, 1, Mat.zeros(1, 3, CvType.CV_64F), 0, pos, 0);
		Core.add(pos, tvec, pos);
		Scalar size = new Scalar(Consts.BOARD_CHECKER_SIZE);
		Core.multiply(pos, size, pos);

		// Log.i(TAG, "Position" + col + "," + row + " " + pos.dump());

		return pos;
	}

	private void calculateRotation(Mat r) {
		// roll = atan2(-R[2][1], R[2][2])
		// pitch = asin(R[2][0])
		// yaw = atan2(-R[1][0], R[0][0])
		// Log.i(TAG, "rotation: " + r.dump());

		mRoll = (Math.atan2(-r.get(2, 1)[0], r.get(2, 2)[0]) * 180) / Math.PI;
		mPitch = (Math.asin(r.get(2, 0)[0]) * 180) / Math.PI;
		mYaw = (Math.atan2(-r.get(1, 0)[0], r.get(0, 0)[0]) * 180) / Math.PI;
		if (PoseInfo.deviceFlipped == 1) {
			mYaw = mYaw + 90;
		}
		// Log.i(TAG, "roll " + mRoll);
		// Log.i(TAG, "pitch " + mPitch);
		// Log.i(TAG, "yaw " + mYaw);
	}

	private void printPosition(Mat r, Mat tvec) {
		for (int i = 0; i < Consts.BOARD_WIDTH; i++) {
			for (int j = 0; j < Consts.BOARD_HEIGHT; j++) {
				mPositions.push_back(calculationPosition(r, tvec, i, 2 * j + i
						% 2, 0));
			}
		}
	}

	private void generate3DCoordinates(MatOfPoint3f objectPoints) {
		for (int i = Consts.BOARD_WIDTH - 1; i >= 0; i--) {
			for (int j = 0; j < Consts.BOARD_HEIGHT; j++) {
				objectPoints.push_back(new MatOfPoint3f(new Point3(i, j * 2 + i
						% 2, 0)));
			}
		}
	}

	private void drawPoints(Mat rgbaFrame) {
		Calib3d.drawChessboardCorners(rgbaFrame, new Size(Consts.BOARD_WIDTH,
				Consts.BOARD_HEIGHT), mCorners, mPatternWasFound);
	}

	public void setPoseEstimationDoneListener(
			OnPoseEstimationDoneListener eventListener) {
		mListener = eventListener;
	}

	public Mat getCameraMatrix() {
		return mCameraMatrix;
	}

	public Mat getDistortionCoefficients() {
		return mDistortionCoefficients;
	}

	public Mat getEyePosition() {
		return mEyePosition;
	}

	public double getAvgReprojectionError() {
		return mRms;
	}

	public boolean isCalibrated() {
		return mIsCalibrated;
	}

	public void setCalibrated() {
		mIsCalibrated = true;
	}

	public boolean getProcessFrameDone() {
		return mProcessFrameDone;
	}

	public void setProcessFrameDone() {
		mProcessFrameDone = false;
	}

	public Mat getPositionsFound() {
		return mPositions;
	}

	public boolean isPatternFound() {
		return mPatternWasFound;
	}

	public double getRoll() {
		return mRoll;
	}

	public double getPitch() {
		return mPitch;
	}

	public double getYaw() {
		return mYaw;
	}
}
