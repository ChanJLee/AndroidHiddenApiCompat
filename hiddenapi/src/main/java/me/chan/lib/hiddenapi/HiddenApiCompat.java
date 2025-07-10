package me.chan.lib.hiddenapi;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class HiddenApiCompat {
	private static boolean sSoLoaded = false;

	public static boolean compat(Context context) {
		// Do nothing below API 28
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return true;
		}

		i("SDK: " + Build.VERSION.SDK_INT + ", Preview SDK: " + Build.VERSION.PREVIEW_SDK_INT);

		// If loading so fails, consider fixing by Java code; this method may not work on all devices
		boolean rtn = compat0(context);
		i("Attempt to fix by setting VM runtime: " + rtn);
		return rtn;
	}

	private static boolean loadLibrary(Context context) {
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
	 * After setting a higher target SDK version, setting VM memory layout no longer works.
	 * Need to fix by setting VM parameters at the Java layer, which has better compatibility.
	 *
	 * @return whether successful
	 */
	private static boolean compat0(Context context) {
		// Direct access may fail
		VmRuntimeReflectBundle vmRuntimeReflectBundle = getVmRuntimeReflectObj();

		// Try to get from JNI layer
		if (vmRuntimeReflectBundle == null) {
			i("Trying to get reflect bundle from JNI");
			vmRuntimeReflectBundle = getVmRuntimeReflectObjCompat(context);
		}

		if (vmRuntimeReflectBundle == null) {
			i("Failed to get reflect bundle");
			return false;
		}

		Method getRuntimeMethod = vmRuntimeReflectBundle.getGetRuntimeMethod();
		Method setHiddenApiExemptions = vmRuntimeReflectBundle.getSetHiddenApiExemptionsMethod();
		if (getRuntimeMethod == null || setHiddenApiExemptions == null) {
			i("fixBySetVmRuntime methods are null");
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

	private static VmRuntimeReflectBundle getVmRuntimeReflectObjCompat(Context context) {
		if (!loadLibrary(context)) {
			i("Failed to load native library");
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
	private static VmRuntimeReflectBundle getVmRuntimeReflectObj() {
		try {
			// Loading hidden API via system classes can bypass checks
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
	 * @param rtnClazz To find the required class in JNI layer, because JNI class loader cannot find our custom class
	 * @param vmClazz  To avoid method lookup exceptions, pass from Java layer
	 * @param <T>      return type
	 * @return return object, must have a public constructor with signature (Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V
	 */
	private static native <T> T getVmRuntimeReflectObjNative(Class<T> rtnClazz, Class<?> vmClazz);

	private static void i(String msg) {
		Log.i("HiddenApiCompat", msg);
	}
}