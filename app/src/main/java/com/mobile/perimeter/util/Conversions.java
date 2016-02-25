package com.mobile.perimeter.util;

public final class Conversions {
	
	public static double cmToInches(double cm) {
		return cm*0.39370;
	}
	
	public static double inchesToCm(double inches) {
		return inches*2.54;
	}
	
	public static double toInches(int pixel, double dpi) {
		return pixel / dpi;
	}

	public static double toPixels(double inches, double dpi) {
		return inches * dpi;
	}

	public static double toRadians(double degrees) {
		return degrees * Math.PI / 180;
	}

	
}
