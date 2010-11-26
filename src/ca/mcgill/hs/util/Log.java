package ca.mcgill.hs.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class Log {

	private static final boolean logToFile = true;

	private static BufferedWriter logWriter;
	static {
		if (logToFile) {
			final File logDir = new File(Environment
					.getExternalStorageDirectory(), "hsandroidapp/log/");
			if (!logDir.isDirectory()) {
				logDir.mkdirs();
			}

			final SimpleDateFormat dfm = new SimpleDateFormat("yy-MM-dd-HHmmss");
			final File logFile = new File(logDir, dfm.format(new Date(System
					.currentTimeMillis()))
					+ ".log");
			try {
				logWriter = new BufferedWriter(new FileWriter(logFile));
			} catch (final IOException e) {
				android.util.Log.e("ca.mcgill.hs.util.Log", android.util.Log
						.getStackTraceString(e));
				logWriter = null;
			}
		} else {
			logWriter = null;
		}
	}

	private final static SimpleDateFormat dfm = new SimpleDateFormat(
			"yy-MM-dd HH:mm:ss");

	public static void d(final String tag, final String msg) {
		log(tag, msg, "D");
		android.util.Log.d(tag, msg);
	}

	public static void d(final String tag, final Throwable e) {
		d(tag, android.util.Log.getStackTraceString(e));
	}

	public static void e(final String tag, final String msg) {
		log(tag, msg, "E");
		android.util.Log.e(tag, msg);
	}

	public static void e(final String tag, final Throwable e) {
		e(tag, android.util.Log.getStackTraceString(e));
	}

	public static void i(final String tag, final String msg) {
		log(tag, msg, "I");
		android.util.Log.e(tag, msg);
	}

	public static void i(final String tag, final Throwable e) {
		i(tag, android.util.Log.getStackTraceString(e));
	}

	private static void log(final String tag, final String msg,
			final String prefix) {
		if (logToFile && logWriter != null) {
			try {
				logWriter.write(dfm
						.format(new Date(System.currentTimeMillis()))
						+ " " + prefix + "/" + tag + ": " + msg + "\n");
				logWriter.flush();
			} catch (final IOException e) {
				android.util.Log.e("ca.mcgill.hs.util.Log", android.util.Log
						.getStackTraceString(e));
			}
		}
	}

	public static void v(final String tag, final String msg) {
		log(tag, msg, "V");
		android.util.Log.e(tag, msg);
	}

	public static void v(final String tag, final Throwable e) {
		v(tag, android.util.Log.getStackTraceString(e));
	}

	public static void w(final String tag, final String msg) {
		log(tag, msg, "W");
		android.util.Log.e(tag, msg);
	}

	public static void w(final String tag, final Throwable e) {
		w(tag, android.util.Log.getStackTraceString(e));
	}

}
