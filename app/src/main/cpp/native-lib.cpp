#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_coursework_cpr_car_1plate_1recognition_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
