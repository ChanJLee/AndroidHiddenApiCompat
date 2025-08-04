//
// Created by chan on 2019/4/22.
//

#ifndef ME_CHAN_LIB_HIDE_API_SCOPE_STRING_H
#define ME_CHAN_LIB_HIDE_API_SCOPE_STRING_H

#include <jni.h>

class ScopeString {
private:
    JNIEnv *mEnv;
    const jstring mJString;
    const char *mCString;

public:
    ScopeString(JNIEnv *env, const jstring str) : mEnv(env), mJString(str) {
        if (mJString != NULL) {
            mCString = mEnv->GetStringUTFChars(mJString, 0);
        }
    }

    virtual ~ScopeString() {
        if (mCString != NULL) {
            mEnv->ReleaseStringUTFChars(mJString, mCString);
        }
    }

    const char *getCString() const {
        return mCString;
    }

    const jstring getJString() const {
        return mJString;
    }
};


#endif //ME_CHAN_LIB_HIDE_API_SCOPE_STRING_H
