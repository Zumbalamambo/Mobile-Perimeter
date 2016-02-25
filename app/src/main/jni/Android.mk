LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)

OPENCV_LIB_TYPE:=SHARED

OPENCV_INSTALL_MODULES:=on

include /Users/ilona/Documents/Android/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk
 
LOCAL_MODULE    := PoseEstimator
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl
 
include $(BUILD_SHARED_LIBRARY)