//
// Created by chan on 2019/4/19.
//

#ifndef ME_CHAN_LIB_HIDE_API_HIDE_API_H
#define ME_CHAN_LIB_HIDE_API_HIDE_API_H

#include <android/log.h>
#include <jni.h>
#include "scope_string.h"
#include "scope_local_obj.h"
#include "scope_global_obj.h"

#define LOG_TAG "HiddenApiCompat"

#define LOGI(...)  ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, \
                   __VA_ARGS__))

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, \
                   __VA_ARGS__))

#define SIZEOF_RUNTIME 2000

#define DEFINE_LOCAL_REF__(RETURN_TYPE, NAME, ENV, OBJ) \
    RETURN_TYPE NAME = (OBJ); \
    ScopeLocalRef<RETURN_TYPE> NAME ## _ref(ENV, NAME);
#define DEFINE_LOCAL_REF(RETURN_TYPE, NAME, ENV, OBJ) DEFINE_LOCAL_REF__(RETURN_TYPE, NAME, ENV, OBJ)

#define DEFINE_GLOBAL_REF__(RETURN_TYPE, NAME, ENV, OBJ) \
    RETURN_TYPE NAME = (OBJ); \
    ScopeGlobalRef<RETURN_TYPE> NAME ## _ref(ENV, NAME);
#define DEFINE_GLOBAL_REF(RETURN_TYPE, NAME, ENV, OBJ) DEFINE_GLOBAL_REF__(RETURN_TYPE, NAME, ENV, OBJ)

#define DEFINE_STR__(NAME, ENV, JSTR) \
    ScopeString NAME ## _scope_str(ENV, JSTR); \
    const char* NAME = NAME ## _scope_str.getCString();

#define DEFINE_STR(NAME, ENV, JSTR) DEFINE_STR__(NAME, ENV, JSTR)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL
Java_me_chan_lib_hiddenapi_HiddenApiCompat_getVmRuntimeReflectObjNative(JNIEnv *env, jclass clazz, jclass return_clazz, jclass vm_clazz);

#ifdef __cplusplus
}
#endif

#endif //ME_CHAN_LIB_HIDE_API_HIDE_API_H