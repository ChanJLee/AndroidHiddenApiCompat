//
// Created by chan on 2019/4/22.
//

#ifndef ME_CHAN_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H
#define ME_CHAN_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H

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

#endif //ME_CHAN_LIB_HIDE_API_SCOPE_LOCAL_OBJ_H
