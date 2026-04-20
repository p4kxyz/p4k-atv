#include <jni.h>
#include <string>
#include <jni.h>

std::string SERVER_URL          = "https://api.phim4k.lol/rest-api/";
std::string API_KEY             = "bbbb411dea44849";

std::string PURCHASE_CODE       = "***********************";


extern "C" jstring
Java_com_files_codes_AppConfig_getApiServerUrl(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SERVER_URL.c_str());
}

extern "C" jstring
Java_com_files_codes_AppConfig_getApiKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(API_KEY.c_str());
}

extern "C" jstring
Java_com_files_codes_AppConfig_getPurchaseCode(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(PURCHASE_CODE.c_str());
}