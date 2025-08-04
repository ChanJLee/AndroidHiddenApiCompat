//
// Created by chan on 2021/8/16.
//

#ifndef ME_CHAN_LIB_HIDE_API_SCOPE_GLOBAL_OBJ_H
#define ME_CHAN_LIB_HIDE_API_SCOPE_GLOBAL_OBJ_H

#include <jni.h>

template<typename Ref>
class ScopeGlobalRef {
private:
    JNIEnv *mEnv;
    const Ref mRef;

public:
    ScopeGlobalRef(JNIEnv *env, const Ref ref) : mEnv(env), mRef(ref) {
    }

    virtual ~ScopeGlobalRef() {
        if (mRef) {
            mEnv->DeleteGlobalRef(mRef);
        }
    }
};

#endif //ME_CHAN_LIB_HIDE_API_SCOPE_GLOBAL_OBJ_H
