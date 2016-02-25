package com.mobile.perimeter.util;

public final class ScreenInfo {
	
	public static int screenHeight;
	public static int screenWidth;
	public static double xDPI;
	public static double yDPI;

		
	public static void setScreenInfo(int height, int width, double xdpi, double ydpi) {
		screenHeight = height;
		screenWidth = width;
		xDPI = xdpi;
		yDPI = ydpi;
	}
	
}
