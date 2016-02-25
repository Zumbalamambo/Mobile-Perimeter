LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := PoseEstimator
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\main\jni\Android.mk \
	C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\main\jni\Application.mk \
	C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\main\jni\jni_part.cpp \

LOCAL_C_INCLUDES += C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\main\jni
LOCAL_C_INCLUDES += C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\arm\jni
LOCAL_C_INCLUDES += C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\debug\jni
LOCAL_C_INCLUDES += C:\Users\Irina K\Documents\_School\ECE 1778 Mobile\_PF21\mobileperimeter\app\src\armDebug\jni

include $(BUILD_SHARED_LIBRARY)
