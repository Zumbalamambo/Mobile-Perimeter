package com.mobile.perimeter.util;


public final class Consts {
	
	public enum GoldmannSize {
		I(0.11), II(0.22), III(0.43), IV(0.86), V(1.72);
		private double value;
 
        private GoldmannSize(double value) {
            this.value = value;
        }
        
        public double val() {
        	return value;
        }
	}

	public enum Stim {
		MISSEDTWO, SEEN, SEENTWO, MISSED, NONE
	}

	public static int USERNAME_KEY = 123456;

	public static int PREV_RESULTS = 2;
	public static int CONT_PREVIOUS = 3;
	public static int DISP_RESULTS = 4;

	public static int MODE = 1;
	public static int LOADED_MODE = -1;
	
	public static boolean MODIFIED_DEMO = false;
	
	public static boolean LIGHTING = true;
	
	public static boolean FULL_TEST = false;
	
	public static boolean DEBUG_MODE = false;
	
	public static int MAX_DB = 0;
	public static int MIN_DB = 34;
	
	public static int BOARD_WIDTH = 7;
	public static int BOARD_HEIGHT = 2;
	public static double BOARD_CHECKER_SIZE = 0.4;

	public static int TEST_SPEED_CONST = 5;


	public static boolean HEAD_TRACKER_MODE = true;
	public static boolean CALIBRATION_MODE = true;
	public static boolean USE_ESTIMATOR_MODE = true;
	
	public static boolean RIGHT_EYE = true;

	public static int startingDb = 16;
	
}
