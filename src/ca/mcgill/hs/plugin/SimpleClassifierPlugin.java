package ca.mcgill.hs.plugin;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.TimeDelayEmbeddingClassifier;
import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.util.PreferenceFactory;

public class SimpleClassifierPlugin extends OutputPlugin {
	private final class LoggerThread extends Thread {
		public Handler mHandler;

		@Override
		public void run() {
			Looper.prepare();

			mHandler = new Handler() {
				@Override
				public void handleMessage(final Message msg) {
					if (msg.what == QUIT_MESSAGE) {
						Looper.myLooper().quit();
						return;
					}
					final int index = msg.arg1;
					// final int timestamp = msg.arg2;
					tdeClassifier.classify(index);
					// Log.d(TAG,"ARV: Logging at timestamp: " + timestamp);
				}
			};

			Looper.loop();
		}
	}

	private final TimeDelayEmbeddingClassifier tdeClassifier;

	private LoggerThread loggerThread;

	private boolean classifying = false;

	// Preference key for this plugin's state
	private final static String PLUGIN_ACTIVE_KEY = "simpleClassifierEnabled";

	private final static String PLUGIN_NAME = "SimpleClassifierPlugin";

	private static final int LOG_MESSAGE = 0;

	private static final int QUIT_MESSAGE = 1;

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				PLUGIN_ACTIVE_KEY, "Simple Classifier Plugin",
				"Enables or disables this plugin.", "SimpleClassifier is on.",
				"SimpleClassifier is off.");
		return prefs;
	}

	/**
	 * Returns whether or not this OutputPlugin has Preferences.
	 * 
	 * @return whether or not this OutputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	private boolean PLUGIN_ACTIVE;

	private final Context context;

	private int counter = 0;

	public SimpleClassifierPlugin(final Context c) {
		context = c;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);

		tdeClassifier = new TimeDelayEmbeddingClassifier();

		PLUGIN_ACTIVE = prefs.getBoolean(PLUGIN_ACTIVE_KEY, false);
	}

	@Override
	void onDataReceived(final DataPacket dp) {
		if (!PLUGIN_ACTIVE || !classifying) {
			return;
		}

		if (dp.getDataPacketId() == SensorLoggerPacket.PLUGIN_ID) {
			final SensorLoggerPacket packet = (SensorLoggerPacket) dp;
			final float x = packet.x;
			final float y = packet.y;
			final float z = packet.z;

			final float m = (float) Math.sqrt(x * x + y * y + z * z)
					- SensorManager.STANDARD_GRAVITY;
			final int index = tdeClassifier.addSample(m);
			counter += 1;
			/*
			 * Classify every 10 samples.
			 */
			if (counter % 10 == 0) {
				final Message msg = loggerThread.mHandler.obtainMessage(
						LOG_MESSAGE, index, (int) packet.time);
				loggerThread.mHandler.sendMessage(msg);
				counter = 0;
			}
		}

	}

	@Override
	protected void onPluginStart() {
		if (PLUGIN_ACTIVE) {
			final Thread t = new Thread() {
				@Override
				public void run() {
					final File modelsFile = new File(Environment
							.getExternalStorageDirectory(), (String) context
							.getResources().getText(R.string.model_ini_path));
					if (modelsFile.canRead()) {
						loggerThread = new LoggerThread();
						loggerThread.start();
						tdeClassifier.loadModels(modelsFile);

						classifying = true;
					} else {
						// No Models.ini file found!
						Log.e(PLUGIN_NAME, "Could not load models.ini from "
								+ modelsFile.getAbsolutePath());
						classifying = false;
					}
				}
			};
			t.start();
		}
	}

	@Override
	protected void onPluginStop() {
		if (classifying) {
			final Message msg = loggerThread.mHandler
					.obtainMessage(QUIT_MESSAGE);
			loggerThread.mHandler.sendMessage(msg);
			classifying = false;
			tdeClassifier.close();
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean(PLUGIN_ACTIVE_KEY,
				false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}
	}

}
