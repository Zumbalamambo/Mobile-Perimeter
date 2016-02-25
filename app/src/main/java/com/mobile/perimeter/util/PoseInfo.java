package com.mobile.perimeter.util;

public final class PoseInfo {
	
	
	public static final double CONST_TRANSLATION_X_INCHES = 4.05;//2.99;
	public static final double CONST_TRANSLATION_Y_INCHES = 1.8576;
	public static final double CONST_TRANSLATION_Z_INCHES = 9.84252;
	
	
	public static double translationX = 4.05;//2.99;
	public static double translationY = 1.8576;
	public static double translationZ = 9.84252;

	public static double rotationYaw = 0;
	public static double rotationPitch = 0;
	public static double rotationRoll = 0;
	
	public static int deviceFlipped = 0;
	
	public static double eyeCoordX = -6;
	public static double eyeCoordY = 1;
	public static double eyeCoordZ = 2;
	
	public static void setPoseInfoCm(double x, double y, double z, double yaw, double pitch, double roll) {
		translationX = Conversions.cmToInches(x);
		translationY = Conversions.cmToInches(y);
		translationZ = Conversions.cmToInches(z);
		rotationYaw = yaw;
		rotationPitch = pitch;
		rotationRoll = roll;

	}
	
	public static void setPoseInfoInches(double x, double y, double z, double yaw, double pitch, double roll) {
		translationX = x;
		translationY = y;
		translationZ = z;
		rotationYaw = yaw;
		rotationPitch = pitch;
		rotationRoll = roll;

	}
}
