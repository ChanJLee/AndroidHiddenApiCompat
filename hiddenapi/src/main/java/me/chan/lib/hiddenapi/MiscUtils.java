package me.chan.lib.hiddenapi;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;

/**
 * Created by chan on 2017/11/8.
 */

public class MiscUtils {
	private static final String TAG = "LogMiscUtils";

	private static String sProcessName;

	@Deprecated
	@Nullable
	public static String getProcessName(@NonNull Context context) {
		return getProcessName();
	}

	@Nullable
	public static String getProcessName() {
		if (!TextUtils.isEmpty(sProcessName)) {
			return sProcessName;
		}

		sProcessName = getProcessNameInternal();
		if (TextUtils.isEmpty(sProcessName)) {
			w("get process name failed, return empty process name");
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
			//ignore
		}
		return processName;
	}

	public static boolean isDebug(Context context) {
		try {
			ApplicationInfo info = context.getApplicationInfo();
			return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		} catch (Exception e) {
			return false;
		}
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
				// lots of '0' in tail, remove them
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

	public static String getMD5(String input) {
		if (TextUtils.isEmpty(input)) {
			return null;
		}

		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(input.getBytes());
			byte[] digest = messageDigest.digest();
			StringBuilder hexString = new StringBuilder();
			for (byte bit : digest) {
				String hex = Integer.toHexString(0xff & bit);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
