package me.chan.lib.hiddenapi;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class HiddenApiCompat {
	private static boolean sSoLoaded = false;

	public static boolean fix(Context context) {
		// 28 以下下不操作
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return true;
		}

		i("sdk : " + Build.VERSION.SDK_INT + " preview sdk: " + Build.VERSION.PREVIEW_SDK_INT);

		// so 加载错误考虑使用java代码修复，此方法不能再所有机器上奏效
		boolean rtn = fixBySetVmRuntime(context);
		i("try to fix by set vm runtime: " + rtn);
		return rtn;
	}

	private static boolean loadIfSo(Context context) {
		if (sSoLoaded) {
			return true;
		}

		LibraryLoader loader = new LibraryLoader("hidden-api", new String[]{
				"hidden-api"
		});
		sSoLoaded = loader.load(context);
		return sSoLoaded;
	}

	/**
	 * 设置高版本target sdk后，设置虚拟机内存布局的代码不再奏效
	 * 需要通过设置java层的 vm 参数来解决，该方法兼容性更好
	 *
	 * @return 是否成功
	 */
	private static boolean fixBySetVmRuntime(Context context) {
		// 直接获取可能获取不到
		VmRuntimeReflectBundle vmRuntimeReflectBundle = getVmRuntimeReflectObjDirect();

		// 尝试从Jni层获取
		if (vmRuntimeReflectBundle == null) {
			i("try to get reflect bundle from jni");
			vmRuntimeReflectBundle = getVmRuntimeReflectObjNative(context);
		}

		if (vmRuntimeReflectBundle == null) {
			i("get reflect failed");
			return false;
		}

		Method getRuntimeMethod = vmRuntimeReflectBundle.getGetRuntimeMethod();
		Method setHiddenApiExemptions = vmRuntimeReflectBundle.getSetHiddenApiExemptionsMethod();
		if (getRuntimeMethod == null || setHiddenApiExemptions == null) {
			i("fixBySetVmRuntime method null");
			return false;
		}

		try {
			getRuntimeMethod.setAccessible(true);
			setHiddenApiExemptions.setAccessible(true);
			Object o = getRuntimeMethod.invoke(null);
			setHiddenApiExemptions.invoke(o, new Object[]{new String[]{"L"}});
			return true;
		} catch (Throwable throwable) {
			Log.w("HiddenApiCompat", throwable);
			return false;
		}
	}

	private static VmRuntimeReflectBundle getVmRuntimeReflectObjNative(Context context) {
		if (!loadIfSo(context)) {
			i("load so failed");
			return null;
		}

		try {
			return getVmRuntimeReflectObjNative(VmRuntimeReflectBundle.class, Class.forName("dalvik.system.VMRuntime"));
		} catch (Throwable e) {
			Log.w("HiddenApiCompat", e);
			return null;
		}
	}

	@Nullable
	private static VmRuntimeReflectBundle getVmRuntimeReflectObjDirect() {
		try {
			// 通过系统类去load hide api可以绕过检查
			Method methodForName = Class.class.getDeclaredMethod("forName", String.class);
			Method methodGetDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

			Class<?> vmRuntimeClass = (Class<?>) methodForName.invoke(null, "dalvik.system.VMRuntime");
			return new VmRuntimeReflectBundle(
					(Method) methodGetDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null),
					(Method) methodGetDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class})
			);
		} catch (Throwable throwable) {
			return null;
		}
	}

	/**
	 * @param rtnClazz 为了在jni层能够查找到我们需要的class，因为实现问题，jni的class loader找不到我们的自定义类
	 * @param vmClazz  为了规避方法查找异常，在jni层找的话还是会触发，所以需要从java层传下去
	 * @param <T>      return 类型
	 * @return return对象，需要满足的结构就是，必须有一个 (Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V 的构造函数，并且是公共的
	 */
	private static native <T> T getVmRuntimeReflectObjNative(Class<T> rtnClazz, Class<?> vmClazz);

	private static void i(String msg) {
		Log.i("HiddenApiCompat", msg);
	}
}
