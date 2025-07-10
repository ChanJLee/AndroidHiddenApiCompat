package me.chan.lib.hiddenapi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibraryLoader {

	private List<String> mLibraries;
	private boolean mLibrariesWereUnpacked = false;
	private String mModuleName;

	/**
	 * @param moduleName The name of the module to load, must be unique
	 * @param libs       The libraries to load
	 */
	public LibraryLoader(String moduleName, String[] libs) {
		mLibraries = new LinkedList<>(Arrays.asList(libs));
		mModuleName = moduleName;
		i("ABI: " + Build.CPU_ABI);
	}

	public synchronized boolean load(Context context) {
		if (mLibraries == null || mLibraries.isEmpty()) {
			i("Libraries have already been loaded, returning true");
			return true;
		}

		boolean result = true;
		String workaroundLibDir = getWorkaroundLibDirName(context);
		i("Workaround directory: " + workaroundLibDir);

		Iterator<String> iterator = mLibraries.iterator();
		while (iterator.hasNext()) {
			String lib = iterator.next();
			if (TextUtils.isEmpty(lib)) {
				continue;
			}

			if (!loadLib(context, workaroundLibDir, lib)) {
				result = false;
				w("Failed to load " + lib);
			} else {
				i("Successfully loaded " + lib);
			}
			iterator.remove();
		}
		return result;
	}

	private String getWorkaroundLibDirName(Context context) {
		String pkg = context.getPackageName();
		String processName = MiscUtils.getProcessName();
		String workaroundLibDir = "lld_v1_" + mModuleName;
		if (TextUtils.equals(pkg, processName)) {
			return workaroundLibDir;
		}

		int start = pkg.length() + 1;
		if (processName == null || start >= processName.length()) {
			return workaroundLibDir + "_unknown_" + android.os.Process.myPid();
		}

		return workaroundLibDir + "_" + processName.substring(start);
	}

	private boolean loadLib(Context context, String workaroundLibDir, String library) {
		try {
			i("Loading library using System.loadLibrary");
			System.loadLibrary(library);
			return true;
		} catch (UnsatisfiedLinkError e) {
			w("Failed to load library using System.loadLibrary", e);
			e.printStackTrace();
		}

		i("Loading library using compatibility method");
		return loadLibCompact(context, workaroundLibDir, library);
	}


	/**
	 * Try to load a native library using a workaround of
	 * http://b/13216167.
	 * <p>
	 * Workaround for b/13216167 was adapted from code in
	 * https://googleplex-android-review.git.corp.google.com/#/c/433061
	 * <p>
	 * More details about http://b/13216167:
	 * PackageManager may fail to update shared library.
	 * <p>
	 * Native library directory in an updated package is a symbolic link
	 * to a directory in /data/app-lib/<package name>, for example:
	 * /data/data/com.android.chrome/lib -> /data/app-lib/com.android.chrome[-1].
	 * When updating the application, the PackageManager creates a new directory,
	 * e.g., /data/app-lib/com.android.chrome-2, and removes the old symlink and
	 * recreates one to the new directory. However, on some devices (e.g. Sony Xperia),
	 * the symlink was updated, but fails to extract new native libraries from
	 * the new apk.
	 * We make the following changes to alleviate the issue:
	 * 1) name the native library with apk version code, e.g.,
	 * libchrome.1750.136.so, 1750.136 is Chrome version number;
	 * 2) first try to load the library using System.loadLibrary,
	 * if that failed due to the library file not found,
	 * search the named library in a /data/data/com.android.chrome/app_lib
	 * directory. Because of change 1), each version has a different native
	 * library name, so avoid mistakenly using the old native library.
	 * <p>
	 * If named library is not in /data/data/com.android.chrome/app_lib directory,
	 * extract native libraries from apk and cache in the directory.
	 * <p>
	 * This function doesn't throw UnsatisfiedLinkError, the caller needs to
	 * check the return value.
	 */
	@SuppressLint("UnsafeDynamicallyLoadedCode")
	private boolean loadLibCompact(Context context, String workaroundLibDir, String library) {
		File libFile = getWorkaroundLibFile(context, workaroundLibDir, library);
		if (!libFile.exists() && !unpackLibrariesOnce(context, workaroundLibDir)) {
			return false;
		}

		try {
			System.load(libFile.getAbsolutePath());
			return true;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
	}

	private boolean unpackLibrariesOnce(Context context, String workaroundLibDir) {
		if (mLibrariesWereUnpacked) {
			return true;
		}

		File libDir = getWorkaroundLibDir(context, workaroundLibDir);
		deleteDirectorySync(libDir);
		try {
			ApplicationInfo appInfo = context.getApplicationInfo();
			ZipFile file = new ZipFile(new File(appInfo.sourceDir), ZipFile.OPEN_READ);
			for (String libName : mLibraries) {
				if (TextUtils.isEmpty(libName)) {
					continue;
				}

				String jniNameInApk = getJniNameInApk(libName);
				final ZipEntry entry = file.getEntry(jniNameInApk);
				if (entry == null) {
					e(appInfo.sourceDir + " doesn't contain file " + jniNameInApk);
					file.close();
					deleteDirectorySync(libDir);
					return false;
				}
				File outputFile = getWorkaroundLibFile(context, workaroundLibDir, libName);
				i("Extracting native library to " + outputFile.getAbsolutePath());
				assert !outputFile.exists();
				try {
					if (!outputFile.createNewFile()) {
						throw new IOException();
					}
					InputStream is = null;
					FileOutputStream os = null;
					try {
						is = file.getInputStream(entry);
						os = new FileOutputStream(outputFile);
						int count = 0;
						byte[] buffer = new byte[16 * 1024];
						while ((count = is.read(buffer)) > 0) {
							os.write(buffer, 0, count);
						}
					} finally {
						try {
							if (is != null) is.close();
						} finally {
							if (os != null) os.close();
						}
					}
					// Change permission to rwxr-xr-x
					outputFile.setReadable(true, false);
					outputFile.setExecutable(true, false);
					outputFile.setWritable(true);
				} catch (IOException e) {
					if (outputFile.exists()) {
						if (!outputFile.delete()) {
							e("Failed to delete " + outputFile.getAbsolutePath());
						}
					}
					file.close();
					throw e;
				}
			}
			file.close();
			mLibrariesWereUnpacked = true;
			return true;
		} catch (IOException e) {
			e("Failed to unpack native libraries", e);
			deleteDirectorySync(libDir);
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	private static String getJniNameInApk(String libName) {
		return "lib/" + Build.CPU_ABI + "/" + System.mapLibraryName(libName);
	}

	private static void deleteDirectorySync(File dir) {
		try {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (!file.delete()) {
						e("Failed to remove " + file.getAbsolutePath());
					}
				}
			}
			if (!dir.delete()) {
				w("Failed to remove " + dir.getAbsolutePath());
			}
		} catch (Exception e) {
			e("Failed to remove old libraries", e);
		}
	}

	private File getWorkaroundLibDir(Context context, String workaroundLibDir) {
		return context.getDir(workaroundLibDir, Context.MODE_PRIVATE);
	}

	private File getWorkaroundLibFile(Context context, String workaroundLibDir, String library) {
		String libName = System.mapLibraryName(library);
		return new File(getWorkaroundLibDir(context, workaroundLibDir), libName);
	}

	private static void i(String msg) {
		Log.i("LibraryLoader", msg);
	}

	private static void w(String msg) {
		Log.w("LibraryLoader", msg);
	}

	private static void e(String msg) {
		Log.e("LibraryLoader", msg);
	}

	private static void e(String msg, Throwable throwable) {
		Log.e("LibraryLoader", msg, throwable);
	}

	private static void w(String msg, Throwable throwable) {
		Log.w("LibraryLoader", msg, throwable);
	}
}