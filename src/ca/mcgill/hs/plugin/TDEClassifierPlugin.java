package ca.mcgill.hs.plugin;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.AccelerometerLingeringFilter;
import ca.mcgill.hs.classifiers.LingeringNotificationWidget;
import ca.mcgill.hs.classifiers.TimeDelayEmbeddingClassifier;
import ca.mcgill.hs.plugin.SensorLogger.SensorPacket;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * Currently classifies motion status into moving or lingering based on a simple
 * threshold over a moving average of the accelerometer magnitudes.
 * 
 * Also classifies according to time-delay embedding models (Frank et al, 2010)
 * if they exist in the models folder.
 */
public final class TDEClassifierPlugin extends OutputPlugin {
	private static final class ClassifierThread extends Thread {
		public static Handler mHandler = null;

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
					final float[] classProbs = tdeClassifier.classify(index);
					for (int i = 0; classProbs != null && i < classProbs.length; i++) {
						cumulativeClassProbs[i] += classProbs[i];
					}
				}
			};

			Looper.loop();
		}
	}

	private final static TimeDelayEmbeddingClassifier tdeClassifier = new TimeDelayEmbeddingClassifier();

	private ClassifierThread classifierThread;

	private boolean classifying = false;

	private long timeLingering = 0;
	private long timeMoving = 0;
	private long timeMovingWithoutStopping = 0;
	private static float[] cumulativeClassProbs = null;

	// Preference key for this plugin's state
	private final static String PLUGIN_ACTIVE_KEY = "tdeClassifierEnabled";
	private final static String ACCEL_THRESHOLD_KEY = "accelerometerThreshold";
	private final static int ACCEL_THRESHOLD_DEFAULT = 100;
	private final static String ACCEL_WINDOW_KEY = "accelerometerWindowSize";
	private final static String ACCEL_WINDOW_DEFAULT = "25";
	public static final String MANAGE_MODELS_PREF = "manageModels";

	private final static String PLUGIN_NAME = "TDEClassifierPlugin";

	private static final int LOG_MESSAGE = 0;
	private static final int QUIT_MESSAGE = 1;

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[4];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				PLUGIN_ACTIVE_KEY, R.string.simpleclassifier_enable_pref_label,
				R.string.simpleclassifier_enable_pref_summary,
				R.string.simpleclassifier_enable_pref_on,
				R.string.simpleclassifier_enable_pref_off, false);

		prefs[1] = PreferenceFactory.getSeekBarPreference(activity,
				getThresholdAttributes(), ACCEL_THRESHOLD_KEY);

		prefs[2] = PreferenceFactory.getListPreference(activity,
				R.array.simpleclassifier_pref_lingering_windowsize_strings,
				R.array.simpleclassifier_pref_lingering_windowsize_values,
				ACCEL_WINDOW_DEFAULT, ACCEL_WINDOW_KEY,
				R.string.accelerometerLingeringWindow,
				R.string.accelerometerLingeringWindowSummary);

		prefs[3] = PreferenceFactory.getButtonPreference(activity,
				MANAGE_MODELS_PREF, R.string.manage_model_files_pref_title,
				R.string.manage_model_files_pref_summary);
		prefs[3].setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				final Intent i = new Intent(activity,
						ca.mcgill.hs.prefs.ManageModelsFileManager.class);
				activity.startActivity(i);
				return true;
			}
		});

		return prefs;
	}

	private static AttributeSet getThresholdAttributes() {
		final XmlResourceParser xpp = PluginFactory
				.getXmlResourceParser(R.xml.lingering_threshold_slider);
		AttributeSet attrs = null;

		try {
			xpp.next();
			int eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG
						&& xpp.getName().equals(
								"ca.mcgill.hs.prefs.SeekBarPreference")) {
					attrs = Xml.asAttributeSet(xpp);
					break;
				}
				eventType = xpp.next();
			}
		} catch (final XmlPullParserException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return attrs;
	}

	/**
	 * Returns whether or not this OutputPlugin has Preferences.
	 * 
	 * @return whether or not this OutputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	private AccelerometerLingeringFilter lingeringFilter;

	private int counter = 0;

	private static Context context;

	private final SharedPreferences prefs;

	private TableRow buttonRow;

	public TDEClassifierPlugin(final Context context) {
		TDEClassifierPlugin.context = context;
		prefs = PreferenceFactory.getSharedPreferences();
	}

	@Override
	void onDataReceived(final DataPacket packet) {
		if (!pluginEnabled || !classifying) {
			return;
		}

		if (packet.getDataPacketId() == SensorPacket.PACKET_ID) {
			final SensorPacket sensorPacket = (SensorPacket) packet;
			final float x = sensorPacket.x;
			final float y = sensorPacket.y;
			final float z = sensorPacket.z;
			final float m = (float) Math.sqrt(x * x + y * y + z * z)
					- SensorManager.STANDARD_GRAVITY;

			final int index = tdeClassifier.addSample(m);
			final boolean moving = lingeringFilter.update(m);
			if (moving) {
				timeMoving += 1;
				timeMovingWithoutStopping += 1;
			} else {
				timeLingering += 1;
				timeMovingWithoutStopping = 0;
			}

			/*
			 * Classify every 5 samples when moving consistently.
			 */
			if (timeMovingWithoutStopping % 5 == 4) {
				// Update widget text
				final Message msg = ClassifierThread.mHandler.obtainMessage(
						LOG_MESSAGE, index, (int) sensorPacket.time);
				ClassifierThread.mHandler.sendMessage(msg);
			}

			/*
			 * Update the widget text every 50 samples.
			 */
			if (counter >= 49) {
				final StringBuffer buf = new StringBuffer();
				buf.append("Lingering: " + timeLingering + "\nMoving: "
						+ timeMoving);
				for (int i = 0; i < cumulativeClassProbs.length; i++) {
					buf.append("\nProb for model " + i + ": "
							+ cumulativeClassProbs[i]);
				}
				LingeringNotificationWidget.updateText(buf.toString());
				counter = 0;
			}
			counter += 1;
		}
	}

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(PLUGIN_ACTIVE_KEY, false);
		if (!pluginEnabled) {
			return;
		}

		// Set up the buttons on the main screen.
		final TableLayout freeSpace = HSAndroid.getFreeSpace();

		/* Create a new row to be added. */
		buttonRow = new TableRow(context);
		buttonRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		final Button buildButton = new Button(context);
		buildButton.setPadding(5, 0, 5, 0);
		buildButton.setText("Build");
		buildButton.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		final Button classifyButton = new Button(context);
		classifyButton.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		classifyButton.setPadding(5, 0, 5, 0);
		classifyButton.setText("Classify");

		buttonRow.addView(buildButton);
		buttonRow.addView(classifyButton);

		/* Add row to TableLayout. */
		freeSpace.addView(buttonRow, new TableLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		Log.d(PLUGIN_NAME, "Added buttons! Freespace has "
				+ freeSpace.getChildCount() + " childs");

		final Thread t = new Thread() {
			@Override
			public void run() {
				final SharedPreferences prefs = PreferenceFactory
						.getSharedPreferences();
				final float threshold = prefs.getInt(ACCEL_THRESHOLD_KEY,
						ACCEL_THRESHOLD_DEFAULT) / 100.0f;

				Log.d(PLUGIN_NAME, "Threshold is: " + threshold);

				final int windowSize = Integer.parseInt(prefs.getString(
						ACCEL_WINDOW_KEY, ACCEL_WINDOW_DEFAULT));
				final File modelsFile = new File(Environment
						.getExternalStorageDirectory(), (String) context
						.getResources().getText(R.string.model_ini_path));
				if (modelsFile.canRead()) {
					classifierThread = new ClassifierThread();
					classifierThread.start();
					tdeClassifier.loadModels(modelsFile);
					lingeringFilter = new AccelerometerLingeringFilter(
							threshold, windowSize);
					cumulativeClassProbs = new float[tdeClassifier
							.getNumModels()];
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

	@Override
	protected void onPluginStop() {
		if (buttonRow != null) {
			((TableLayout) buttonRow.getParent()).removeView(buttonRow);
			buttonRow = null;
		}
		if (classifying) {
			final Message msg = ClassifierThread.mHandler
					.obtainMessage(QUIT_MESSAGE);
			ClassifierThread.mHandler.sendMessage(msg);
			classifying = false;
			tdeClassifier.close();
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(PLUGIN_ACTIVE_KEY,
				false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		}

		final float threshold = prefs.getInt(ACCEL_THRESHOLD_KEY,
				ACCEL_THRESHOLD_DEFAULT) / 100.0f;
		final int windowSize = Integer.parseInt(prefs.getString(
				ACCEL_WINDOW_KEY, ACCEL_WINDOW_DEFAULT));
		if (lingeringFilter != null) {
			lingeringFilter.setThreshold(threshold);
			lingeringFilter.setWindowSize(windowSize);
		}
	}

}
