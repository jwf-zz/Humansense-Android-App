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
					final int b = msg.arg1;
					// final int timestamp = msg.arg2;
					// Log.d(TAG,"ARV: Logging at timestamp: " + timestamp);
					final float[] buf = new float[windowLength];
					synchronized (buffer) {
						for (int i = 0; i < windowLength; i++) {
							buf[i] = buffer[b - windowLength + 1 + i];
						}
					}
					classifySample(buf, 0, classProbs);
					String message = "Class Probs: ";
					for (int i = 0; i < classProbs.length; i++) {
						message = message + classProbs[i] + "\t";
					}
					Log.d(PLUGIN_NAME, message);
				}
			};

			Looper.loop();
		}
	}

	private LoggerThread loggerThread;

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

	// Circular buffer for data.
	private float[] buffer;
	private int bufferIndex = 0;
	private int bufferMidPoint;
	private int bufferLength;
	private int windowLength;
	private float[] classProbs;

	private int counter = 0;

	private boolean PLUGIN_ACTIVE;

	private final Context context;

	private boolean modelsLoaded = false;

	static {
		System.loadLibrary("humansense");
	}

	// ******************************************************************** //
	// Native Methods
	// ******************************************************************** //

	public SimpleClassifierPlugin(final Context c) {
		context = c;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);

		PLUGIN_ACTIVE = prefs.getBoolean(PLUGIN_ACTIVE_KEY, false);
	}

	public native void annClose();

	public native float annDist(int dim, float[] p, float[] q);

	// Builds a model from data in in_file with embedding dimension m,
	// PCA reduced dimension p, and delay time d. Model is saved in a file
	// constructed from in_file with .dmp appended.
	public native void buildTree(String in_file, int m, int p, int d);

	// Classifies the data in the array in, starting from index offset, and
	// returned values are stored in output, which must be an array of length
	// getNumModels()
	public native void classifySample(float[] in, int startIndex, float[] out);

	// Loads models from models_file, then classifies data from in_file,
	// storing the class-likelihoods in out_file.
	public native void classifyTrajectory(String in_file, String out_file,
			String models_file);

	// Cleans up any loaded models.
	public native void deleteModels();

	// Returns a tab-separated list of model names
	public native String getModelNames();

	// Returns the number of loaded models
	public native int getNumModels();

	// Returns the minimum number of samples that must be passed to the
	// classifySamples.
	// It is the maximum of the window sizes required for all of the models.
	public native int getWindowSize();

	// Loads models from models_file.
	public native void loadModels(String models_file);

	@Override
	void onDataReceived(final DataPacket dp) {
		if (!PLUGIN_ACTIVE || !modelsLoaded) {
			return;
		}

		if (dp.getDataPacketId() == SensorLoggerPacket.PLUGIN_ID) {
			final SensorLoggerPacket packet = (SensorLoggerPacket) dp;
			final float x = packet.x;
			final float y = packet.y;
			final float z = packet.z;

			final float m = (float) Math.sqrt(x * x + y * y + z * z)
					- SensorManager.STANDARD_GRAVITY;
			synchronized (buffer) {
				buffer[bufferIndex] = m;
				if (bufferIndex >= bufferMidPoint) {
					if (bufferIndex > bufferMidPoint) {
						buffer[bufferIndex - windowLength] = m;
					}
					counter += 1;
					if (counter % 10 == 0) {
						final Message msg = loggerThread.mHandler
								.obtainMessage(LOG_MESSAGE, bufferIndex,
										(int) packet.time);
						loggerThread.mHandler.sendMessage(msg);
						counter = 0;
					}
				}
				bufferIndex++;
				if (bufferIndex >= bufferLength) {
					bufferIndex = bufferMidPoint;
				}
			}
		}

	}

	@Override
	protected void onPluginStart() {
		if (PLUGIN_ACTIVE) {
			final Thread t = new Thread() {
				@Override
				public void run() {
					final File j = new File(Environment
							.getExternalStorageDirectory(), (String) context
							.getResources().getText(R.string.model_ini_path));
					if (j.canRead()) {
						loadModels(j.getAbsolutePath());

						// Prepare the buffer
						windowLength = getWindowSize();
						bufferLength = windowLength * 2 - 1;
						bufferMidPoint = windowLength - 1;
						buffer = new float[bufferLength];
						bufferIndex = 0;
						classProbs = new float[getNumModels()];

						loggerThread = new LoggerThread();
						loggerThread.start();

						modelsLoaded = true;
						Log.d(PLUGIN_NAME, "Loaded " + getNumModels()
								+ " models.");
						Log.d(PLUGIN_NAME, "Window Size Is " + getWindowSize());
					} else {
						// No Models.ini file found!
						Log.e(PLUGIN_NAME, "Could not load models.ini from "
								+ j.getAbsolutePath());
						modelsLoaded = false;
					}
				}
			};
			t.start();
		}
	}

	@Override
	protected void onPluginStop() {
		if (modelsLoaded) {
			final Message msg = loggerThread.mHandler
					.obtainMessage(QUIT_MESSAGE);
			loggerThread.mHandler.sendMessage(msg);
			modelsLoaded = false;
			annClose();
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
