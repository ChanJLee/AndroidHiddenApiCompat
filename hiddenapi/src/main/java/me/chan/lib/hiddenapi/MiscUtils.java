package me.chan.lib.hiddenapi;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.Nullable;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.lang.reflect.Method;

/**
 * Created by chan on 2017/11/8.
 */

public class MiscUtils {
	private static final String TAG = "LogMiscUtils";

	private static String sProcessName;

	@Nullable
	public static String getProcessName() {
		if (!TextUtils.isEmpty(sProcessName)) {
			return sProcessName;
		}

		sProcessName = getProcessNameInternal();
		if (TextUtils.isEmpty(sProcessName)) {
			w("Failed to get process name, returning empty string");
			return "";
		}

		return sProcessName;
	}

	@Nullable
	private static String getProcessNameInternal() {
		String currentProcess = getProcessNameAboveP();
		if (!TextUtils.isEmpty(currentProcess)) {
			return currentProcess;
		}

		int myPid = android.os.Process.myPid();
		currentProcess = getProcessNameFromCmdlineFile(myPid);
		if (!TextUtils.isEmpty(currentProcess)) {
			return currentProcess;
		}

		return getProcessNameByHookActivityThread();
	}

	@Nullable
	private static String getProcessNameAboveP() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return null;
		}

		try {
			return Application.getProcessName();
		} catch (Throwable t) {
			// ignore
		}
		return null;
	}

	private static String getProcessNameByHookActivityThread() {
		String processName = null;
		try {
			@SuppressLint("PrivateApi")
			Class<?> activityThread = Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader());
			Method declaredMethod = activityThread.getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
			declaredMethod.setAccessible(true);
			Object processInvoke = declaredMethod.invoke(null);
			if (processInvoke instanceof String) {
				processName = (String) processInvoke;
			}
		} catch (Throwable e) {
			// ignore
		}
		return processName;
	}

	@Nullable
	private static String getProcessNameFromCmdlineFile(int pid) {
		FileInputStream in = null;
		try {
			in = new FileInputStream("/proc/" + pid + "/cmdline");
			byte[] buffer = new byte[256];
			int len = in.read(buffer);
			if (len <= 0) {
				return null;
			}

			for (int i = 0; i < len; i++) {
				// Remove trailing zeros
				if ((((int) buffer[i]) & 0xFF) > 128 || buffer[i] <= 0) {
					len = i;
					break;
				}
			}
			return new String(buffer, 0, len);
		} catch (Throwable e) {
			w(e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Throwable e) {
				w(e);
			}
		}
		return null;
	}

	private static void w(String msg) {
		Log.w(TAG, msg);
	}

	private static void w(Throwable throwable) {
		Log.w(TAG, throwable);
	}

	private static void i(String msg) {
		Log.i(TAG, msg);
	}
}