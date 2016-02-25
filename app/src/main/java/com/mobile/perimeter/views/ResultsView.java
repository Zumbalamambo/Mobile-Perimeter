package com.mobile.perimeter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mobile.perimeter.database.ResultsIntoDatabase;
import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.ScreenInfo;
import com.mobile.perimeter.views.primitives.Quadrant;
import com.mobile.perimeter.views.primitives.Stimulus;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ResultsView extends View {

	private ArrayList<Quadrant> mQuads;
	private Paint mColour;
	private Paint mBackgroundColour;
	private ResultsIntoDatabase mResultDatabase;
	private String mResults;
	private Calendar mCalendar;
	private SimpleDateFormat mDate;
	private double mMeanSensitivity;
	private boolean afterTestDisplay;
	private boolean mRightEye;

	public ResultsView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ResultsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onDraw(Canvas c) {
		int numStimuli = 0;

		if (afterTestDisplay) {
			mBackgroundColour.setColor(Color.GRAY);
		} else {
			mBackgroundColour.setColor(Color.WHITE);
		}
		mBackgroundColour.setStyle(Style.FILL);
		c.drawPaint(mBackgroundColour);

		mColour.setColor(Color.BLACK);
		c.drawLine(0, (ScreenInfo.screenHeight / 2) - 100,
				ScreenInfo.screenWidth, (ScreenInfo.screenHeight / 2) - 100,
				mColour);
		c.drawLine(ScreenInfo.screenWidth / 2, 0, ScreenInfo.screenWidth / 2,
				ScreenInfo.screenHeight, mColour);

		for (Quadrant q : mQuads) {
			mRightEye = q.getEye();
			for (Stimulus s : q.getStimuli()) {
				int colour = s.getDiffSensitivity() * 7;

				// was used to test out inverting colours hypothesis
				// colour = 255 - colour;

				int diffSensitivity;

				if (s.getDone() == 0) {
					mColour.setColor(Color.BLUE);
					diffSensitivity = -1;
				} else {
					mColour.setColor(Color.rgb(colour, colour, colour));
					diffSensitivity = s.getDiffSensitivity();

					if (!((s.getStimCoordX() == 3 && s.getStimCoordY() == -1) || (s
							.getStimCoordX() == 3 && s.getStimCoordY() == 1))) {
						// Calculation for mean sensitivity
						numStimuli++;
						mMeanSensitivity += diffSensitivity;
					}
				}

				int cx = (int) convertToScreenPixel(0, s.getStimCoordX());
				int cy = (int) convertToScreenPixel(1, s.getStimCoordY());
				
				

				if (s.getIsDuplicate()) {
					cy = cy + 30;
				}
				c.drawCircle(cx, cy, 20, mColour);
				Log.d("results",
						"quad: " + q.getQuadId() + " sx: " + s.getStimCoordX()
								+ " sy: " + s.getStimCoordY() + " cx: " + cx
								+ " cy: " + cy);

				mColour.setColor(Color.DKGRAY);
				mColour.setTextSize(25);
				c.drawText(Integer.toString(diffSensitivity), cx - 12, cy + 10,
						mColour);
			}
		}

		mMeanSensitivity = Math.round((mMeanSensitivity / numStimuli) * 100.0) / 100.0;

		mColour.setColor(Color.BLACK);
		String MS = "MS = " + Double.toString(mMeanSensitivity);
		c.drawText(MS, ScreenInfo.screenWidth - 300,
				ScreenInfo.screenHeight - 300, mColour);

	}

	public void set_quad_info(ArrayList<Quadrant> quads, boolean recordDataOrNot) {
		mColour = new Paint();
		mBackgroundColour = new Paint();

		mMeanSensitivity = 0;
		mQuads = quads;
		afterTestDisplay = recordDataOrNot;
		

		// only record data into database if required
		if (recordDataOrNot) {
			mResultDatabase = new ResultsIntoDatabase(getContext());
			mResultDatabase.open();

			mCalendar = Calendar.getInstance();
			mDate = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss a");
			mResults = "";
			for (Quadrant q : mQuads) {
				for (Stimulus s : q.getStimuli()) {
					int diffSensitivity;

					if (s.getDone() == 0) {
						diffSensitivity = -1;
					} else {
						diffSensitivity = s.getDiffSensitivity();
					}
					String result = "quad " + q.getQuadId() + " ("
							+ s.getStimCoordX() + "," + s.getStimCoordY()
							+ "): " + diffSensitivity + "; ";

					if (s.getIsDuplicate()) {
						mResults += "(d) ";
					}
					mResults += result;
				}
			}
			String strDate = mDate.format(mCalendar.getTime());

			mResultDatabase.createEntry(strDate, Integer.toString(Consts.MODE),
					mResults);
			mResultDatabase.close();
			try {
				mResultDatabase.copyAppDbToExternalStorage(getContext());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		invalidate();
	}

	private double convertToScreenPixel(int xOrY, double coord) {
		double midX = ScreenInfo.screenWidth / 2;
		double midY = ScreenInfo.screenHeight / 2;

		// X == 0, Y == 1
		if (xOrY == 0) {
			if (!mRightEye) {
				coord = -coord; 
			}
			return getScreenPixel(coord, midX, 10);
		} else {
			return getScreenPixel(-coord, midY, 7) - 100;
		}

	}

	private double getScreenPixel(double coord, double mid, double constant) {
		if (coord > 0) {
			return (double) coord * (mid / constant) + mid
					- (mid / (2 * constant));
		} else if (coord < 0) {
			return (double) coord * (mid / constant) + mid
					+ (mid / (2 * constant));
		} else {
			return (double) coord * (mid / constant) + mid;
		}
	}
}
