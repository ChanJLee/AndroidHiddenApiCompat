//
// Created by chan on 2019/4/19.
//

#include <vector>
#include "hidden_api.h"
#include <unistd.h>
#include <thread>
#include <future>

#ifdef __cplusplus
extern "C" {
#endif

// vm operation
JavaVM *_vm;
class VMAttachHolder {
    JNIEnv *env;
    JavaVM *vm;
public:
    VMAttachHolder(JavaVM *vm) : vm(vm) {
        int res = vm->AttachCurrentThread(&env, nullptr);
        LOGI("vm attached result: %d", res);
    }

    ~VMAttachHolder() {
        vm->DetachCurrentThread();
    }

    JNIEnv *getEnv() const {
        return env;
    }
};

jobject getVmRuntimeReflectObjNativeAsync(jclass return_obj_clazz, jclass vm_runtime_clazz) {
    VMAttachHolder holder(_vm);
    JNIEnv *env = holder.getEnv();
    LOGD("vm runtime clazz, %p", vm_runtime_clazz);

    DEFINE_LOCAL_REF(jclass, obj_clazz, env, env->FindClass("java/lang/Class"));
    LOGD("obj clazz, %p", obj_clazz);

    jmethodID get_declared_method_id = env->GetMethodID(obj_clazz, "getDeclaredMethod",
                                                        "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
    LOGD("get declared method, %p", get_declared_method_id);

    // 1. getRuntime
    DEFINE_LOCAL_REF(jstring, get_runtime_str, env, env->NewStringUTF("getRuntime"));
    jobjectArray empty_args = nullptr;
    jobject getRuntimeMethodObj = env->CallObjectMethod(vm_runtime_clazz, get_declared_method_id,
                                                        get_runtime_str, empty_args);
    LOGD("get runtime method, %p", getRuntimeMethodObj);

    // 2. setHiddenApiExemptions
    DEFINE_LOCAL_REF(jstring, setHiddenApiExemptionsStr, env,
                     env->NewStringUTF("setHiddenApiExemptions"));
    DEFINE_LOCAL_REF(jclass, string_arr_clazz, env, env->FindClass("[Ljava/lang/String;"))
    LOGD("string array class, %p", string_arr_clazz);
    DEFINE_LOCAL_REF(jobjectArray, setHiddenApiExemptionsArgs, env,
                     env->NewObjectArray(1, obj_clazz, string_arr_clazz))
    jobject setHiddenApiExemptionsMethodObj = env->CallObjectMethod(vm_runtime_clazz,
                                                                    get_declared_method_id,
                                                                    setHiddenApiExemptionsStr,
                                                                    setHiddenApiExemptionsArgs);
    LOGD("set hidden api exemptions method, %p", setHiddenApiExemptionsMethodObj);

    jmethodID ctor_method_id = env->GetMethodID(return_obj_clazz, "<init>",
                                                "(Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V");
    LOGD("return obj's ctor_method_id %p", ctor_method_id);
    jobject rtn = env->NewObject(return_obj_clazz, ctor_method_id, getRuntimeMethodObj,
                                 setHiddenApiExemptionsMethodObj);
    LOGD("return obj's %p", rtn);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    return env->NewGlobalRef(rtn);
}

JNIEXPORT jobject JNICALL
Java_me_chan_lib_hiddenapi_HiddenApiCompat_getVmRuntimeReflectObjNative(JNIEnv *env,
                                                                            jclass type,
                                                                            jclass return_clazz,
                                                                            jclass vm_clazz) {
    DEFINE_GLOBAL_REF(jobject, global_rtn_clazz, env, env->NewGlobalRef(return_clazz))
    DEFINE_GLOBAL_REF(jobject, global_vm_clazz, env, env->NewGlobalRef(vm_clazz))
    auto feature = std::async(&getVmRuntimeReflectObjNativeAsync,
                              static_cast<jclass>(global_rtn_clazz),
                              static_cast<jclass>(global_vm_clazz));
    auto result = feature.get();
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    return result;
}

#ifdef __cplusplus
}
#endif

jint JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    _vm = vm;
    return JNI_VERSION_1_4;
}