
#include <jni.h>
#include <vector>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/core.hpp>

using namespace cv;

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_edgedetect_MainActivity_processFrame(JNIEnv *env, jobject /* this */, jbyteArray input_, jint width, jint height, jint mode) {
    jbyte *input = env->GetByteArrayElements(input_, nullptr);
    int len = env->GetArrayLength(input_);

    // NV21 -> BGR conversion
    Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*)input);
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);

    Mat gray;
    cvtColor(bgr, gray, COLOR_BGR2GRAY);

    Mat out;
    if (mode == 1) {
        // Edge detect
        Mat edges;
        Canny(gray, edges, 80, 200);
        cvtColor(edges, out, COLOR_GRAY2RGBA);
    } else {
        cvtColor(gray, out, COLOR_GRAY2RGBA);
    }

    // Ensure RGBA ordering (uchar per channel)
    int outSize = out.total() * out.elemSize();
    jbyteArray result = env->NewByteArray(outSize);
    env->SetByteArrayRegion(result, 0, outSize, reinterpret_cast<jbyte*>(out.data));

    env->ReleaseByteArrayElements(input_, input, JNI_ABORT);
    return result;
}
