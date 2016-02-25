package com.mobile.perimeter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.perimeter.bluetooth.BleService;
import com.mobile.perimeter.bluetooth.DeviceListFragment;
import com.mobile.perimeter.database.InfoDbHelper;
import com.mobile.perimeter.light.LightUtil;
import com.mobile.perimeter.poseestimation.CalibrationResult;
import com.mobile.perimeter.poseestimation.OnPoseEstimationDoneListener;
import com.mobile.perimeter.poseestimation.PoseEstimator;
import com.mobile.perimeter.settings.SettingsActivity;
import com.mobile.perimeter.util.Consts;
import com.mobile.perimeter.util.Debugging;
import com.mobile.perimeter.util.PoseInfo;
import com.mobile.perimeter.util.ScreenInfo;
import com.mobile.perimeter.util.SystemUiHider;
import com.mobile.perimeter.views.CalibrationView;
import com.mobile.perimeter.views.DemoView;
import com.mobile.perimeter.views.PerimeterView;
import com.mobile.perimeter.views.PoseView;
import com.mobile.perimeter.views.ResultsView;
import com.mobile.perimeter.views.primitives.CalibrationListener;
import com.mobile.perimeter.views.primitives.PerimetryDoneListener;
import com.mobile.perimeter.views.primitives.Quadrant;

public class FullscreenActivity extends FragmentActivity implements
		DeviceListFragment.OnDeviceListFragmentInteractionListener,
		PerimetryDoneListener, CvCameraViewListener2,
		OnPoseEstimationDoneListener, CalibrationListener {

	private static final boolean AUTO_HIDE = true;
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider = null;

	public static final String TAG = "BluetoothLE";
	private final int ENABLE_BT = 1;
	private boolean USE_SELECTION_DATA = false;

	private boolean storeLostData = false;

	private final Messenger mMessenger;
	private Intent mServiceIntent;
	private Messenger mService = null;
	private BleService.State mState = BleService.State.UNKNOWN;

	private MenuItem mRefreshItem = null;
	private int menuCase = 0;
	static PerimeterView mPerimeterView;
	static ResultsView mResultsView;
	static PoseView mPoseView;
	static DemoView mDemoView;
	static CalibrationView mCalibrationView;
	private DeviceListFragment mDeviceList = DeviceListFragment.newInstance();

	private Button mStartButton;
	private Button mBlindspotButton;
	private Button mBackButton;
	private Button mDemoButton;

	private LinearLayout mMenuScreen;
	private GridLayout mAdjustEyeDistance;

	private CameraBridgeViewBase mOpenCvCameraView;
	private static final String CAMERA_CALIBRATION_DATA = "CameraCalibrationData";
	private boolean mProcessThisFrame = false;
	private int mWidth;
	private int mHeight;
	private PoseEstimator mEstimator;

	private int mCountOfPoseFailures;
	private int mCountOfPoseEstimates;

	WindowManager wm;

	public FullscreenActivity() {
		super();
		mMessenger = new Messenger(new IncomingHandler(this));
	}

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

		wm = (WindowManager) getApplicationContext().getSystemService(
				Context.WINDOW_SERVICE);

		final DisplayMetrics metrics = new DisplayMetrics();

		Display display = wm.getDefaultDisplay();

		display.getRealMetrics(metrics);

		ScreenInfo.setScreenInfo(metrics.heightPixels, metrics.widthPixels,
				metrics.xdpi, metrics.ydpi);

		mPerimeterView = (PerimeterView) findViewById(R.id.perimeter_view);
		mPoseView = (PoseView) findViewById(R.id.pose_view);
		mDemoView = (DemoView) findViewById(R.id.demo_view);
		mResultsView = (ResultsView) findViewById(R.id.results_view);
		mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);
		mStartButton = (Button) findViewById(R.id.start_button);
		mBlindspotButton = (Button) findViewById(R.id.blindspot_button);
		mBackButton = (Button) findViewById(R.id.back_button);
		mDemoButton = (Button) findViewById(R.id.demo_button);

		mAdjustEyeDistance = (GridLayout) findViewById(R.id.adjust_eye_distance);
		mMenuScreen = (LinearLayout) findViewById(R.id.menu_screen);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

	}

	public void undoFullScreen() {
		ActionBar actionbar = getActionBar();
		actionbar.show();

		mHideRunnable = null;

		if (mSystemUiHider != null) {
			mSystemUiHider.show();
			mSystemUiHider.setOnVisibilityChangeListener(null);
		}
		// mSystemUiHider = null;
		final View contentView = findViewById(R.id.main_content);
		contentView.setOnClickListener(null);
		TOGGLE_ON_CLICK = false;
	}

	public void makeFullScreen() {
		// make full screen
		ActionBar actionbar = getActionBar();
		actionbar.hide();

		TOGGLE_ON_CLICK = true;
		mHideRunnable = new Runnable() {
			@Override
			public void run() {
				mSystemUiHider.hide();
			}
		};

		final View contentView = findViewById(R.id.main_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {

							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}

						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		mSystemUiHider.hide();

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

	}

	private void hideViews() {
		mMenuScreen.setVisibility(View.INVISIBLE);
		mBackButton.setVisibility(View.INVISIBLE);
		mPoseView.setVisibility(View.INVISIBLE);
		mDemoView.setVisibility(View.INVISIBLE);
		mPerimeterView.setVisibility(View.INVISIBLE);
		mStartButton.setVisibility(View.GONE);
		mBlindspotButton.setVisibility(View.GONE);
		mDemoButton.setVisibility(View.GONE);
		mAdjustEyeDistance.setVisibility(View.INVISIBLE);
		mCalibrationView.setVisibility(View.INVISIBLE);
		mResultsView.setVisibility(View.INVISIBLE);

	}

	public void startDemo(View v) {
		makeFullScreen();

		hideViews();
		mBackButton.setVisibility(View.VISIBLE);

		mDemoView.setVisibility(View.VISIBLE);
		mDemoView.startDemoDisplay();

		if (Consts.DEBUG_MODE) {
			mPoseView.setVisibility(View.VISIBLE);
			mPoseView.startPoseDisplay();
		}

		mProcessThisFrame = true;

	}

	public void setMaxBrightness() {
		// setting screen brightness to max
		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
		layoutParams.screenBrightness = 1.0f;
		getWindow().setAttributes(layoutParams);

	}

	public void startPerimetry() {
		mPerimeterView.setVisibility(View.VISIBLE);

		mPerimeterView.setPerimetryDoneListener(this);
		mPerimeterView.setCalibrationDoneListener(this);
		// if we aren't using the selection data, create a blank set of
		// quadrants and stimuli
		// if (USE_SELECTION_DATA == 0) {
		mPerimeterView.createQuadsAndStimuli(null, USE_SELECTION_DATA,
				Consts.RIGHT_EYE);
		// } else {
		// if we are using the selection data, set it so that next time we start
		// a new test, it won't use past data
		USE_SELECTION_DATA = false;
		// }

		Debugging.appendLog("Perimetry test start for user:"
				+ Consts.USERNAME_KEY);
		storeLostData = true;

		mCountOfPoseFailures = 0;
		mCountOfPoseEstimates = 0;

		mPerimeterView.startPerimetry();

	}

	LightUtil lighting;

	public void startPerimetryTest(View v) {

		setMaxBrightness();
		lighting = new LightUtil(getApplicationContext());

		PoseInfo.deviceFlipped = 0;

		if (v == mStartButton) {
			Consts.MODE = 2;
		} else if (v == mBlindspotButton) {
			Consts.MODE = 1;
		}

		hideViews();

		// start processing frames (if the head pose is activated)
		mProcessThisFrame = true;
		if (Consts.DEBUG_MODE) {
			mPoseView.setVisibility(View.VISIBLE);
			mPoseView.startPoseDisplay();
		}
		// start calibration if head tracker mode on, else start the perimetry
		if (Consts.HEAD_TRACKER_MODE) {
			mAdjustEyeDistance.setVisibility(View.VISIBLE);

		} else {
			makeFullScreen();
			startPerimetry();
		}

	}

	public void onLeftClick(View v) {
		PoseInfo.eyeCoordX = PoseInfo.eyeCoordX + 0.3;
	}

	public void onRightClick(View v) {
		PoseInfo.eyeCoordX = PoseInfo.eyeCoordX - 0.3;
	}

	public void onUpClick(View v) {
		PoseInfo.eyeCoordY = PoseInfo.eyeCoordY - 0.3;
	}

	public void onDownClick(View v) {
		PoseInfo.eyeCoordY = PoseInfo.eyeCoordY + 0.3;
	}

	public void onEyeDistanceCalibrated(View v) {
		mAdjustEyeDistance.setVisibility(View.INVISIBLE);
		makeFullScreen();

		PoseInfo.setPoseInfoCm(0, 0, 0, PoseInfo.rotationYaw,
				PoseInfo.rotationPitch, PoseInfo.rotationRoll);
		mCalibrationView.setVisibility(View.VISIBLE);
		mCalibrationView.setCalibrationDoneListener(this);
		mCalibrationView.startCalibrationDisplay();
	}

	public void startPastSelection(View v) {
		storeLostData = false;
		Intent i = new Intent(this, SelectPrevious.class);
		this.startActivityForResult(i, Consts.PREV_RESULTS);
		;

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable;

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	private void loadPreferences() {
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		// ----------------------------------------------------
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		Consts.RIGHT_EYE = preferences.getBoolean("right_or_left_eye", true);

		Consts.FULL_TEST = preferences.getBoolean("full_or_abbreviated_test",
				true);

		Consts.LIGHTING = preferences.getBoolean("dark_or_lit_room", true);

		Consts.DEBUG_MODE = preferences.getBoolean("debug_mode", false);

		Consts.HEAD_TRACKER_MODE = preferences.getBoolean("head_tracking_mode",
				true);

		Consts.CALIBRATION_MODE = preferences.getBoolean("calibration_mode",
				true);

		Consts.USE_ESTIMATOR_MODE = preferences.getBoolean(
				"use_estimator_mode", true);

		Consts.BOARD_WIDTH = Integer.valueOf(preferences.getString("box_width",
				"7"));

		Consts.BOARD_HEIGHT = Integer.valueOf(preferences.getString(
				"box_height", "2"));

		Consts.BOARD_CHECKER_SIZE = ((double) Integer.valueOf(preferences
				.getString("box_size", "4"))) / 10.0;

		Consts.USERNAME_KEY = Integer.valueOf(preferences.getString(
				"username_key", "123456"));

		Consts.TEST_SPEED_CONST = Integer.valueOf(preferences.getString(
				"test_speed", "5"));

		PoseInfo.eyeCoordX = -6.0 / Consts.BOARD_CHECKER_SIZE;

		if (!Consts.RIGHT_EYE) {
			PoseInfo.eyeCoordX = -PoseInfo.eyeCoordX;
		}
		
		Consts.startingDb = Integer.valueOf(preferences.getString(
				"starting_db", "16"));
		
		Consts.MODIFIED_DEMO = preferences.getBoolean("modified_demo", false);

	}

	@Override
	protected void onResume() {

		Log.d("onFUNC", "inside onresume");
		super.onResume();

		if (mPerimeterView.isPaused() && !mPerimeterView.getPerimetryDone()) {
			resume();
		}

		loadPreferences();

	}

	@Override
	protected void onDestroy() {

		Log.d("onFUNC", "inside ondestroy");
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		// pv.set_stopped(true);
		if (mService != null) {
			try {
				Message msg = Message.obtain(null, BleService.MSG_UNREGISTER);
				if (msg != null) {
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
			} catch (Exception e) {
				Log.w(TAG, "Error unregistering with BleService", e);
				mService = null;
			} finally {
				unbindService(mConnection);
			}
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d("onFUNC", "inside onpause");

		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		if (storeLostData) {
			mPerimeterView.setInterrupted(true);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			if (mService != null) {
				startScan();
			}
			return true;
		} else if (id == R.id.action_settings) {
			// startBle();
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (menuCase == 0) {
			getMenuInflater().inflate(R.menu.start_screen, menu);
		} else {
			getMenuInflater().inflate(R.menu.main, menu);
			mRefreshItem = menu.findItem(R.id.action_refresh);

		}
		if (mRefreshItem != null) {
			mRefreshItem.setEnabled(mState == BleService.State.IDLE
					|| mState == BleService.State.UNKNOWN);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				startScan();
			} else {
				// The user has elected not to turn on
				// Bluetooth. There's nothing we can do
				// without it, so let's finish().
				finish();
			}
		} else if (requestCode == Consts.PREV_RESULTS) {

			if (resultCode == Consts.CONT_PREVIOUS
					|| resultCode == Consts.DISP_RESULTS) {
				Bundle b = data.getExtras();
				Consts.LOADED_MODE = Integer.parseInt(b
						.getString(InfoDbHelper.COLUMN_MODE));
				String results = b.getString(InfoDbHelper.COLUMN_RESULTS);

				int eye = b.getInt(InfoDbHelper.COLUMN_EYE);
				boolean currEye = false;
				if (eye == 1) {
					currEye = true;
				}
				mPerimeterView.createQuadsAndStimuli(results, false, currEye);
				if (resultCode == Consts.CONT_PREVIOUS) {
					USE_SELECTION_DATA = true;
				} else if (resultCode == Consts.DISP_RESULTS) {
					onPerimetryDone(mPerimeterView.getQuads(), false);
				}
			}

		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	// When the perimetry is finished, display the results and undo the full
	// screen that was
	// used for the perimetry part
	public void onPerimetryDone(ArrayList<Quadrant> quads,
			boolean recordDataOrNot) {
		undoFullScreen();
		if (lighting != null) {
			lighting.stopLightSensor();
		}
		PoseInfo.deviceFlipped = 0;

		Debugging
				.appendLog("Pose Failures:"
						+ mCountOfPoseFailures
						+ "/"
						+ mCountOfPoseEstimates
						+ " = "
						+ 100.0
						* ((float) mCountOfPoseFailures / (float) mCountOfPoseEstimates)
						+ "%");

		mCountOfPoseFailures = 0;
		mCountOfPoseEstimates = 0;

		hideViews();

		mResultsView.setVisibility(View.VISIBLE);
		mResultsView.set_quad_info(quads, recordDataOrNot);
		storeLostData = false;
		mBackButton.setVisibility(View.VISIBLE);
	}

	// When pressing the done/back button, the user will go back to the original
	// perimetry screen with the correct buttons visible
	public void backFunction(View v) {
		undoFullScreen();

		mProcessThisFrame = false;

		if (mPoseView.isShown()) {
			mPoseView.stopPoseDisplay();
		}

		if (mDemoView.isShown()) {
			mDemoView.stopDemoDisplay();
		}
		hideViews();

		mMenuScreen.setVisibility(View.VISIBLE);
		mStartButton.setVisibility(View.VISIBLE);
		mBlindspotButton.setVisibility(View.VISIBLE);
		mDemoButton.setVisibility(View.VISIBLE);
	}

	// When the user exist the app and comes back in, show them the beginning
	// screen
	public void resume() {
		undoFullScreen();
		if (lighting != null) {
			lighting.stopLightSensor();
		}
		hideViews();

		mMenuScreen.setVisibility(View.VISIBLE);
		mStartButton.setVisibility(View.VISIBLE);
		mBlindspotButton.setVisibility(View.VISIBLE);
		mDemoButton.setVisibility(View.VISIBLE);
	}

	// -------------- BLUETOOTH STUFF ------------------------------
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, BleService.MSG_REGISTER);
				if (msg != null) {
					msg.replyTo = mMessenger;
					mService.send(msg);
				} else {
					mService = null;
				}
			} catch (Exception e) {
				Log.w(TAG, "Error connecting to BleService", e);
				mService = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	public void onDeviceListFragmentInteraction(String macAddress) {
		Message msg = Message.obtain(null, BleService.MSG_DEVICE_CONNECT);
		if (msg != null) {
			msg.obj = macAddress;
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Lost connection to service", e);
				unbindService(mConnection);
			}
		}
	}

	public void startBluetoothSearch(View v) {
		mServiceIntent = new Intent(this, BleService.class);
		FragmentTransaction tx = getFragmentManager().beginTransaction();
		tx.add(R.id.main_content, mDeviceList);
		tx.addToBackStack(null);
		tx.commit();
		menuCase = 1;

		invalidateOptionsMenu();
		bindService(mServiceIntent, mConnection, BIND_AUTO_CREATE);

	}

	private void startScan() {
		mRefreshItem.setEnabled(true);
		mDeviceList.setDevices(this, null);
		mDeviceList.setScanning(true);
		Message msg = Message.obtain(null, BleService.MSG_START_SCAN);
		if (msg != null) {
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Lost connection to service", e);
				unbindService(mConnection);
			}
		}
	}

	private static class IncomingHandler extends Handler {
		private final WeakReference<FullscreenActivity> mActivity;

		public IncomingHandler(FullscreenActivity activity) {
			mActivity = new WeakReference<FullscreenActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			FullscreenActivity activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
				case BleService.MSG_STATE_CHANGED:
					activity.stateChanged(BleService.State.values()[msg.arg1]);
					break;
				case BleService.MSG_DEVICE_FOUND:
					Bundle data = msg.getData();
					if (data != null
							&& data.containsKey(BleService.KEY_MAC_ADDRESSES)) {
						activity.mDeviceList.setDevices(activity, data
								.getStringArray(BleService.KEY_MAC_ADDRESSES));
					}
					break;
				case BleService.MSG_DEVICE_DATA:
					Log.d("bluetooth", "keypressed!");

					if (mPerimeterView != null) {
						mPerimeterView.setPressed();
					}
					break;
				}
			}
			super.handleMessage(msg);
		}
	}

	private void stateChanged(BleService.State newState) {
		boolean disconnected = mState == BleService.State.CONNECTED;
		mState = newState;
		switch (mState) {
		case SCANNING:
			mRefreshItem.setEnabled(true);
			mDeviceList.setScanning(true);
			break;
		case BLUETOOTH_OFF:
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, ENABLE_BT);
			break;
		case IDLE:
			if (disconnected) {
				FragmentTransaction tx = getFragmentManager()
						.beginTransaction();
				tx.replace(R.id.main_content, mDeviceList);
				tx.addToBackStack(null);
				tx.commit();
			}
			if (getFragmentManager().getBackStackEntryCount() != 0) {
				mRefreshItem.setEnabled(true);
				mDeviceList.setScanning(false);
			} else {
				menuCase = 0;
				invalidateOptionsMenu();
			}
			break;
		case CONNECTED:
			Toast.makeText(getApplicationContext(), "Connected",
					Toast.LENGTH_SHORT).show();
			menuCase = 0;
			mDeviceList.close();
			invalidateOptionsMenu();
			break;
		}
	}

	// ------------------ OPENCV Camera + Pose stuff -------------------------
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				System.loadLibrary("PoseEstimator");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public void onCameraViewStarted(int width, int height) {
		if (mWidth != width || mHeight != height) {
			mWidth = width;
			mHeight = height;
			mEstimator = new PoseEstimator(mWidth, mHeight);
			mEstimator.setPoseEstimationDoneListener(this);

			SharedPreferences settings = getSharedPreferences(
					CAMERA_CALIBRATION_DATA, 0);

			if (settings.getFloat("0", -1) == -1) {
				Log.i(TAG, "No previous calibration results found");
				CalibrationResult.storeCameraData(this);
			}
			CalibrationResult.loadCameraData(this,
					mEstimator.getCameraMatrix(),
					mEstimator.getDistortionCoefficients());

		}
	}

	public void onCameraViewStopped() {
		Log.i(TAG, "onCameraViewStopped");
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		final Mat grayFrame = inputFrame.gray();

		if (mProcessThisFrame
				&& (Consts.HEAD_TRACKER_MODE || Consts.DEBUG_MODE)) {
			mProcessThisFrame = false;
			mEstimator.processFrame(grayFrame);
		}

		Core.flip(grayFrame, grayFrame, 1);
		return grayFrame;
	}

	@Override
	public void onProcessFrameDone() {
		Log.i(TAG, "Process frame done.");

		if (!mMenuScreen.isShown()) {
			mProcessThisFrame = true;
			mCountOfPoseEstimates++;
			if (mEstimator.isPatternFound()) {

				if (mDemoView.isShown()) {
					Log.i(TAG, "demo view shown");
					mDemoView.refreshDemoView();
				}

				if (Consts.DEBUG_MODE) {
					// print out x, y, z translation and roll pitch yaw
					mPoseView.refreshPoseEstimation();
				}

			}
			if (mEstimator.isPatternFound()) {
				Debugging.appendLog("Pose found");
			} else {
				mCountOfPoseFailures++;
				Debugging.appendLog("Pose not found");
			}
			Debugging.appendLog("Pose:" + PoseInfo.translationX + ","
					+ PoseInfo.translationY + "," + PoseInfo.translationZ
					+ " -- " + PoseInfo.rotationPitch + ","
					+ PoseInfo.rotationRoll + "," + PoseInfo.rotationYaw + ",");

		}
	}

	@Override
	public void onBackPressed() {
		if (getFragmentManager().getBackStackEntryCount() != 0) {
			getFragmentManager().popBackStack();

			// so far, only one fragment, assume bluetooth one got backpressed,
			// now readjust the menu again
			menuCase = 0;
			invalidateOptionsMenu();
		} else {
			super.onBackPressed();
		}
	}

	public void onCalibrationEvent(int event) {
		// EVENT = 1 : CALIBRATION DONE
		if (event == 1) {
			mAdjustEyeDistance.setVisibility(View.INVISIBLE);
			mCalibrationView.setVisibility(View.INVISIBLE);
			mCalibrationView.stopCalibrationDisplay();

			mPerimeterView.setVisibility(View.VISIBLE);
			if (mPerimeterView.isPaused()) {
				mPerimeterView.onCalibrationDone();
			} else {
				startPerimetry();
			}

		} else if (event == 0) {
			mPerimeterView.setPaused(true);
			mCalibrationView.setVisibility(View.VISIBLE);
			mPerimeterView.setVisibility(View.INVISIBLE);
			mCalibrationView.startCalibrationDisplay();
		}

	}

}
