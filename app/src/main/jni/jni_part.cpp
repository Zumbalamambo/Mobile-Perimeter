#include <jni.h>
#include "opencv2/core/core.hpp"
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <stdio.h>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <cv.h>

//using namespace std;
//using namespace cv;

extern "C" {

JNIEXPORT void JNICALL Java_com_mobile_perimeter_poseestimation_PoseEstimator_FindChessboard(JNIEnv*, jobject, jlong addrGray, jlong addrPtvec, jint boardHeight, jint boardWidth);

};

JNIEXPORT void JNICALL Java_com_mobile_perimeter_poseestimation_PoseEstimator_FindChessboard(JNIEnv*, jobject, jlong addrGray, jlong addrPtvec, jint boardHeight, jint boardWidth)
{
    cv::Mat& mGr  = *(cv::Mat*)addrGray;
    cv::Mat& mPtvec = *(cv::Mat*)addrPtvec;
    cv::Size boardSize = cvSize(boardHeight, boardWidth);


    findCirclesGrid(mGr, boardSize, mPtvec, cv::CALIB_CB_ASYMMETRIC_GRID);

}
