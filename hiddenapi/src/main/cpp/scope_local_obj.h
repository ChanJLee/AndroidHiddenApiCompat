//
// Created by chan on 2019/4/22.
//

#ifndef SHANBAY_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H
#define SHANBAY_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H

#include <jni.h>

template<typename Ref>
class ScopeLocalRef {
private:
    JNIEnv *mEnv;
    const Ref mRef;

public:
    ScopeLocalRef(JNIEnv *env, const Ref ref) : mEnv(env), mRef(ref) {
    }

    virtual ~ScopeLocalRef() {
        if (mRef) {
            mEnv->DeleteLocalRef(mRef);
        }
    }
};

#endif //SHANBAY_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H
