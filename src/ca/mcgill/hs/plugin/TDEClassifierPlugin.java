/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.TableRow.LayoutParams;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.AccelerometerLingeringFilter;
import ca.mcgill.hs.classifiers.TimeDelayEmbeddingClassifier;
import ca.mcgill.hs.graph.MagnitudeGraph;
import ca.mcgill.hs.network.LogServerClient;
import ca.mcgill.hs.plugin.SensorLogger.SensorPacket;
import ca.mcgill.hs.prefs.ManageModelsFileManager;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;
import ca.mcgill.hs.widget.LingeringNotificationWidget;

/**
 * Currently classifies motion status into moving or lingering based on a simple
 * threshold over a moving average of the accelerometer magnitudes.
 * 
 * Also classifies according to time-delay embedding models (Frank et al, 2010)
 * if they exist in the models folder.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public final class TDEClassifierPlugin extends OutputPlugin {
	/**
	 * Thread for the classifier, the Handler gets notified with the starting
	 * index in the data buffer, and passes that on to the classifier. If
	 * connected to a remote logging server, the class probabilities are sent to
	 * the server.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
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
						if (remoteLoggingClientConnected) {
							try {
								remoteLoggingClassOutputStream
										.writeFloat(classProbs[i]);
							} catch (final IOException e) {
								// TODO Auto-generated catch block
								Log.e(PLUGIN_NAME, e);
							}

						}
					}
				}
			};

			Looper.loop();
		}
	}

	private final static TimeDelayEmbeddingClassifier tdeClassifier = new TimeDelayEmbeddingClassifier();

	private ClassifierThread classifierThread;

	private boolean classifying = false;
	private boolean building = false;

	private long timeLingering = 0;
	private long timeMoving = 0;
	private long timeMovingWithoutStopping = 0;
	private static float[] cumulativeClassProbs = null;

	// Preference keys
	private final static String PLUGIN_ACTIVE_KEY = "tdeClassifierEnabled";
	private final static String ACCEL_THRESHOLD_KEY = "accelerometerThreshold";
	private final static int ACCEL_THRESHOLD_DEFAULT = 100;
	private final static String ACCEL_WINDOW_KEY = "accelerometerWindowSize";
	private final static String ACCEL_WINDOW_DEFAULT = "25";
	public static final String MANAGE_MODELS_PREF = "manageModels";
	private static final String ENABLE_REMOTE_LOGGING_KEY = "tdeClassifierEnableRemoteLogging";
	private static final String REMOTE_LOGGING_HOST_KEY = "tdeClassifierRemoteLoggingHost";
	private final static String PLUGIN_NAME = "TDEClassifierPlugin";
	private static final int LOG_MESSAGE = 0;
	private static final int QUIT_MESSAGE = 1;

	/**
	 * @see OutputPlugin#getPreferences(PreferenceActivity)
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[6];

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

		prefs[4] = PreferenceFactory
				.getCheckBoxPreference(
						activity,
						ENABLE_REMOTE_LOGGING_KEY,
						R.string.simpleclassifier_enable_remote_logging_pref_label,
						R.string.simpleclassifier_enable_remote_logging_pref_summary,
						R.string.simpleclassifier_enable_remote_logging_pref_on,
						R.string.simpleclassifier_enable_remote_logging_pref_off,
						false);

		prefs[5] = PreferenceFactory.getEditTextPreference(activity,
				REMOTE_LOGGING_HOST_KEY,
				R.string.simpleclassifier_remote_logging_host_pref_label,
				R.string.simpleclassifier_remote_logging_host_pref_summary,
				R.string.simpleclassifier_remote_logging_host_pref_dialogmsg,
				R.string.simpleclassifier_remote_logging_host_pref_dialogtitle,
				"");
		return prefs;
	}

	/**
	 * Get the attributes for the seek-bar (slider)
	 * 
	 * @return The set of attributes for the threshold slider.
	 */
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
			Log.e(PLUGIN_NAME, e);
		} catch (final IOException e) {
			Log.e(PLUGIN_NAME, e);
		}
		return attrs;
	}

	/**
	 * @see OutputPlugin#hasPreferences()
	 */
	public static boolean hasPreferences() {
		return true;
	}

	private AccelerometerLingeringFilter lingeringFilter;

	private int counter = 0;

	private static Context context;

	private final SharedPreferences prefs;

	private TableRow buttonRow;

	private BufferedWriter modelFileWriter = null;

	private File modelFile = null;

	private final OnClickListener startBuildingModel = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (classifying) {
				makeToast("Cannot build a new model while classifying");
				return;
			}

			final Button buildButton = (Button) v;

			if (building) {
				Log
						.e(PLUGIN_NAME,
								"ERROR: Building requested while building already in progress.");
			} else {
				try {
					modelFile = File.createTempFile("model", ".dat");
					modelFileWriter = new BufferedWriter(new FileWriter(
							modelFile));
					startTimeStamp = System.currentTimeMillis();
					building = true;
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					Log.e(PLUGIN_NAME, e);
				}

			}

			buildButton.setText(R.string.stop_building_button_text);
			buildButton.setOnClickListener(stopBuildingModel);
		}
	};

	private final OnClickListener stopBuildingModel = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			final Button buildButton = (Button) v;
			finishBuilding();
			buildButton.setText(R.string.start_building_button_text);
			buildButton.setOnClickListener(startBuildingModel);
		}
	};

	private final OnClickListener startClassifying = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			if (building) {
				makeToast("Cannot classify while a model is being built");
				return;
			}
			final Button classifyButton = (Button) v;

			final Thread t = new Thread() {
				@Override
				public void run() {
					final float threshold = prefs.getInt(ACCEL_THRESHOLD_KEY,
							ACCEL_THRESHOLD_DEFAULT) / 100.0f;

					Log.d(PLUGIN_NAME, "Threshold is: " + threshold);

					final int windowSize = Integer.parseInt(prefs.getString(
							ACCEL_WINDOW_KEY, ACCEL_WINDOW_DEFAULT));
					final File modelsFile = ManageModelsFileManager.MODELS_INI_FILE;
					if (modelsFile.canRead()) {
						loadModelNames();
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
					if (classifying && enableRemoteLogging) {
						synchronized (TDEClassifierPlugin.class) {
							if (sshForwardingEnabled) {
								remoteLoggingClient.connect();
								remoteLoggingClientConnected = remoteLoggingClient
										.isConnected();
								if (remoteLoggingClientConnected) {
									remoteLoggingClassOutputStream = remoteLoggingClient
											.classify(modelNames);
									remoteLoggingCDataOutputStream = remoteLoggingClient
											.cdata(modelNames);
								}
							}
						}
					}
				}

			};
			t.start();

			classifyButton.setText(R.string.stop_classifying_button_text);
			classifyButton.setOnClickListener(stopClassifying);
		}
	};

	private final OnClickListener stopClassifying = new OnClickListener() {

		@Override
		public void onClick(final View v) {
			final Button classifyButton = (Button) v;

			if (remoteLoggingClientConnected) {
				final int N = modelNames.size();
				try {
					for (int i = 0; i < N; i++) {
						remoteLoggingClassOutputStream
								.writeFloat(Float.MIN_VALUE);
					}
					remoteLoggingCDataOutputStream.writeFloat(Float.MIN_VALUE);
					remoteLoggingClient.disconnect();
					remoteLoggingClientConnected = false;
				} catch (final IOException e) {
				} finally {
					remoteLoggingClientConnected = false;
				}
			}
			classifying = false;

			classifyButton.setText(R.string.start_classifying_button_text);
			classifyButton.setOnClickListener(startClassifying);
		}
	};

	private static final LogServerClient remoteLoggingClient = new LogServerClient();

	private static boolean remoteLoggingClientConnected = false;

	private List<String> modelNames = null;

	private static DataOutputStream remoteLoggingClassOutputStream = null;
	private static DataOutputStream remoteLoggingCDataOutputStream = null;

	private long startTimeStamp;
	private long endTimeStamp;

	private boolean enableRemoteLogging;

	private boolean sshForwardingEnabled;
	private static Process tunnel = null;

	/**
	 * Construct a classifier plugin.
	 * 
	 * @param context
	 *            The application context, required in order to access
	 *            preferences and the files used by the classifier.
	 */
	public TDEClassifierPlugin(final Context context) {
		TDEClassifierPlugin.context = context;
		prefs = PreferenceFactory.getSharedPreferences(context);
	}

	/**
	 * Displays the raw sensor data captured during the model-building process,
	 * allowing the user to select a subsegment to be used for the model.
	 * 
	 * @throws IOException
	 */
	private void displayModelData() throws IOException {
		// First read in time series data from model file.
		final BufferedReader reader = new BufferedReader(new FileReader(
				modelFile));
		final List<Float> dataList = new LinkedList<Float>();
		String line;
		while ((line = reader.readLine()) != null) {
			dataList.add(new Float(line));
		}
		final Float[] data = dataList.toArray(new Float[0]);
		final int[] labels = new int[data.length];
		MagnitudeGraph.setValues(data, labels);
		MagnitudeGraph.setStartTimestamp(startTimeStamp);
		MagnitudeGraph.setEndTimestamp(endTimeStamp);
		MagnitudeGraph.disableLegend();
		MagnitudeGraph.enableCloseAfterOneActivity();
		MagnitudeGraph
				.setOnGraphClosed(new MagnitudeGraph.GraphClosedRunnable() {
					/*
					 * When the view is closed, get the selected segment and
					 * build the model.
					 */
					private String label;
					private float[] data = new float[0];

					@Override
					public void run() {
						try {
							final File modelFile = new File(
									ManageModelsFileManager.MODELS_DIR, label
											+ ".dat");
							final BufferedWriter writer = new BufferedWriter(
									new FileWriter(modelFile));
							for (int i = 0; i < data.length; i++) {
								writer.write(Float.toString(data[i]) + "\n");
							}
							writer.flush();
							writer.close();
							tdeClassifier.buildModel(modelFile
									.getAbsolutePath(), 7, 7, 3);
							makeToast("Saved model for activity: " + label);
						} catch (final IOException e) {
							Log.e(PLUGIN_NAME, e);
						}
					}

					@Override
					public void setLabelData(final String label,
							final float[] data) {
						this.label = label;
						this.data = data;
					}
				});

		final PendingIntent graphIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, MagnitudeGraph.class), 0);
		try {
			graphIntent.send();
		} catch (final CanceledException e) {
			Log.e(PLUGIN_NAME, e);
		}
	}

	/**
	 * Stop collecting data and display the view with the collected sensor data.
	 */
	private void finishBuilding() {
		synchronized (modelFileWriter) {
			building = false;
			endTimeStamp = System.currentTimeMillis();
			try {
				modelFileWriter.flush();
				modelFileWriter.close();
				displayModelData();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				Log.e(PLUGIN_NAME, e);
			}
		}
	}

	public float[] getCumulativeClassProbs() {
		return cumulativeClassProbs;
	}

	public List<String> getModelNames() {
		return modelNames;
	}

	public long getTimeLingering() {
		return timeLingering;
	}

	public long getTimeMoving() {
		return timeMoving;
	}

	/**
	 * Parses the models.ini file and populates the modelNames list with the
	 * model names.
	 */
	private void loadModelNames() {
		final File modelsFile = ManageModelsFileManager.MODELS_INI_FILE;
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					modelsFile));
			modelNames = new ArrayList<String>();
			String line;
			while ((line = reader.readLine()) != null) {
				final File f = new File(line);
				modelNames.add(f.getName().split("\\.")[0]);
			}
		} catch (final FileNotFoundException e) {
			Log.e(PLUGIN_NAME, e);
		} catch (final IOException e) {
			Log.e(PLUGIN_NAME, e);
		}

	}

	/**
	 * Helper method for notifying the user.
	 * 
	 * @param message
	 *            Message to be displayed briefly.
	 */
	protected void makeToast(final String message) {
		final Toast slice = Toast
				.makeText(context, message, Toast.LENGTH_SHORT);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	@Override
	void onDataReceived(final DataPacket packet) {
		/* Only interested in sensor packets. */
		if (!pluginEnabled
				|| packet.getDataPacketId() != SensorPacket.PACKET_ID) {
			return;
		}
		if (building) {
			/*
			 * If we are building a new model, then we want to compute the
			 * accelerometer magnitude and save that in our model file.
			 */
			final SensorPacket sensorPacket = (SensorPacket) packet;
			final Float m = (float) Math.sqrt(sensorPacket.x * sensorPacket.x
					+ sensorPacket.y * sensorPacket.y + sensorPacket.z
					* sensorPacket.z)
					- SensorManager.STANDARD_GRAVITY;
			try {
				modelFileWriter.write(m.toString() + "\n");
			} catch (final IOException e) {
				Log.e(PLUGIN_NAME, e);
			}
		} else if (classifying) {
			/*
			 * If we are classifying, then we want to compute the accelerometer
			 * magnitude, add it to the classifier's buffer and check to see if
			 * we're moving or stationary.
			 */
			final SensorPacket sensorPacket = (SensorPacket) packet;
			final Float m = (float) Math.sqrt(sensorPacket.x * sensorPacket.x
					+ sensorPacket.y * sensorPacket.y + sensorPacket.z
					* sensorPacket.z)
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
				LingeringNotificationWidget.updateText(timeLingering,
						timeMoving, modelNames, cumulativeClassProbs);
				// LingeringNotificationWidget.updateText(buf.toString());
				counter = 0;
			}
			counter += 1;
		}
	}

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(PLUGIN_ACTIVE_KEY, false);
		enableRemoteLogging = prefs
				.getBoolean(ENABLE_REMOTE_LOGGING_KEY, false);
		if (!pluginEnabled) {
			return;
		}

		setupButtons();
		if (enableRemoteLogging) {
			setupSSHForwarding();
		}
	}

	@Override
	protected void onPluginStop() {
		if (buttonRow != null) {
			((TableLayout) buttonRow.getParent()).removeView(buttonRow);
			buttonRow = null;
		}
		if (building) {
			finishBuilding();
		}
		if (classifying) {
			final Message msg = ClassifierThread.mHandler
					.obtainMessage(QUIT_MESSAGE);
			ClassifierThread.mHandler.sendMessage(msg);
			classifying = false;
			tdeClassifier.close();
		}
		if (sshForwardingEnabled) {
			tearDownSSHForwarding();
		}
	}

	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(PLUGIN_ACTIVE_KEY,
				false);
		final boolean enableRemoteLoggingNew = prefs.getBoolean(
				PLUGIN_ACTIVE_KEY, false);

		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		} else if (enableRemoteLogging && !enableRemoteLoggingNew) {
			// Stop logging remotely
			enableRemoteLogging = false;
			tearDownSSHForwarding();
		} else if (!enableRemoteLogging && enableRemoteLoggingNew) {
			// Start logging remotely
			setupSSHForwarding();
			enableRemoteLogging = true;
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

	/**
	 * Adds buttons to the free space in the main view.
	 */
	private void setupButtons() {
		final TableLayout freeSpace = HSAndroid.getFreeSpace();

		/* Create a new row to be added. */
		buttonRow = new TableRow(context);
		buttonRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		final Button buildButton = new Button(context);
		buildButton.setPadding(5, 0, 5, 0);
		buildButton.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		buildButton.setText(R.string.start_building_button_text);
		buildButton.setOnClickListener(startBuildingModel);
		buttonRow.addView(buildButton);

		final Button classifyButton = new Button(context);
		classifyButton.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		classifyButton.setPadding(5, 0, 5, 0);
		classifyButton.setText(R.string.start_classifying_button_text);
		classifyButton.setOnClickListener(startClassifying);
		buttonRow.addView(classifyButton);

		/* Add row to TableLayout. */
		freeSpace.addView(buttonRow, new TableLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

	}

	private void setupSSHForwarding() {
		final Thread setupSSHThread = new Thread() {
			@Override
			public void run() {
				synchronized (TDEClassifierPlugin.class) {
					try {
						final Runtime rt = Runtime.getRuntime();
						final String serverHost = prefs.getString(
								REMOTE_LOGGING_HOST_KEY, "");
						if (serverHost.length() > 0) {
							Log.d(PLUGIN_NAME, "Connecting to host: "
									+ serverHost);
							final String[] envp = { "HOME=/sdcard/hsandroidapp" };
							tunnel = rt
									.exec(
											"/data/local/bin/ssh -f -y -N -T -L 12021:localhost:12021 -i /sdcard/hsandroidapp/id_rsa "
													+ serverHost, envp);
							tunnel.waitFor();
							sshForwardingEnabled = true;
							Log.d(PLUGIN_NAME, "Connected to server "
									+ serverHost);
						} else {
							Log.d(PLUGIN_NAME,
									"Not connecting, host name is blank.");
						}
					} catch (final IOException e) {
						Log
								.d(PLUGIN_NAME,
										"Error setting up SSH tunnel. Cannot find ssh command in /data/local/bin.");
						Log.e(PLUGIN_NAME, e);
					} catch (final InterruptedException e) {
						Log.e(PLUGIN_NAME, e);
					}
				}
			}
		};
		setupSSHThread.start();
	}

	private void tearDownSSHForwarding() {
		Log.d(PLUGIN_NAME, "Tearing down SSH tunnel.");
		final Runtime rt = Runtime.getRuntime();
		if (tunnel != null) {
			try {
				rt.exec("/data/local/busybox killall -9 ssh");
			} catch (final IOException e) {
				Log
						.d(PLUGIN_NAME,
								"Error tearing down SSH tunnel. Cannot find busybox command in /data/local.");
				Log.e(PLUGIN_NAME, e);
			} finally {
				tunnel.destroy();
			}
		}
		sshForwardingEnabled = false;
	}
}
