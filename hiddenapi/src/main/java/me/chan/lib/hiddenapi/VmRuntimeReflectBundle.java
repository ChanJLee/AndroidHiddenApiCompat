package me.chan.lib.hiddenapi;

import androidx.annotation.RestrictTo;

import java.lang.reflect.Method;

/**
 * do not modify it
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class VmRuntimeReflectBundle {
	private final Method mGetRuntimeMethod;
	private final Method mSetHiddenApiExemptionsMethod;

	// jni 协议要求有这个构造函数
	// 就不keep了，保持代码的私密
	public VmRuntimeReflectBundle(Method getRuntimeMethod, Method setHiddenApiExemptionsMethod) {
		mGetRuntimeMethod = getRuntimeMethod;
		mSetHiddenApiExemptionsMethod = setHiddenApiExemptionsMethod;
	}

	public Method getSetHiddenApiExemptionsMethod() {
		return mSetHiddenApiExemptionsMethod;
	}

	public Method getGetRuntimeMethod() {
		return mGetRuntimeMethod;
	}
}
