/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * An InputPlugin which gets data from the phone's hardware sensors.
 */
public class SensorLogger extends InputPlugin implements SensorEventListener {
	public static class SensorPacket implements DataPacket {

		final long time;
		final float x;
		final float y;
		final float z;
		final float m;
		final float temperature;
		final float[] magfield;
		final float[] orientation;

		final static String PACKET_NAME = "SensorPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

		public SensorPacket(final long time, final float x, final float y,
				final float z, final float m, final float temperature,
				final float[] magfield, final float[] orientation) {
			this.time = time;
			this.x = x;
			this.y = y;
			this.z = z;
			this.m = m;
			this.temperature = temperature;
			this.magfield = magfield;
			this.orientation = orientation;
		}

		@Override
		public DataPacket clone() {
			return new SensorPacket(time, x, y, z, m, temperature, magfield,
					orientation);
		}

		@Override
		public int getDataPacketId() {
			return SensorPacket.PACKET_ID;
		}

		@Override
		public String getInputPluginName() {
			return SensorPacket.PACKET_NAME;
		}

	}

	private static final String SENSOR_LOGGER_INTERVAL_PREF = "sensorIntervalPreference";
	private static final String SENSOR_LOGGER_DEFAULT_INTERVAL = "0";
	private static final String SENSOR_LOGGER_ENABLE_PREF = "sensorLoggerEnablePreference";

	final static String PLUGIN_NAME = "SensorLogger";
	final static int PLUGIN_ID = PLUGIN_NAME.hashCode();
	private static final String SENSOR_LOGGER_CALCULATE_ORIENTATION = "calculateOrientationPreference";

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @return An array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[3];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				SENSOR_LOGGER_ENABLE_PREF,
				R.string.sensorlogger_enable_pref_label,
				R.string.sensorlogger_interval_pref_summary,
				R.string.sensorlogger_enable_pref_on,
				R.string.sensorlogger_enable_pref_off, false);

		prefs[1] = PreferenceFactory.getListPreference(activity,
				R.array.sensorlogger_pref_frequency_strings,
				R.array.sensorlogger_pref_frequency_values,
				SENSOR_LOGGER_DEFAULT_INTERVAL, SENSOR_LOGGER_INTERVAL_PREF,
				R.string.sensorlogger_interval_pref,
				R.string.sensorlogger_interval_pref_summary);

		prefs[2] = PreferenceFactory.getCheckBoxPreference(activity,
				SENSOR_LOGGER_CALCULATE_ORIENTATION,
				R.string.sensorlogger_orientation_pref,
				R.string.sensorlogger_orientation_pref_summary,
				R.string.sensorlogger_orientation_pref_on,
				R.string.sensorlogger_orientation_pref_off, true);
		return prefs;
	}

	public static boolean hasPreferences() {
		return true;
	}

	// The SensorManager used to register listeners.
	private final SensorManager sensorManager;

	// A boolean checking whether or not we are logging at a given moment.
	private boolean logging = false;
	// The speed at which the accelerometer will log.
	private int loggingSpeed;
	// Offset for timestamps
	private long timestamp_offset = 0;
	// Variables used to write out the sensor data received.
	private float temperature = 0.0f;

	private float[] magfield = { 0.0f, 0.0f, 0.0f };

	private boolean magfieldUpdated = false;

	private final float[] orientation = { 0.0f, 0.0f, 0.0f };

	private final SharedPreferences prefs;

	private boolean calculateOrientation;

	/**
	 * This is the basic constructor for the SensorLogger plugin.
	 * 
	 * @param context
	 *            The application context.
	 */
	public SensorLogger(final Context context) {
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);

		prefs = PreferenceFactory.getSharedPreferences(context);
	}

	/**
	 * Processes the results sent by the Sensor change and writes them out.
	 */
	private void logAccelerometerData(final float[] values, final long timestamp) {
		final float x = values[0];
		final float y = values[1];
		final float z = values[2];
		final float m = (float) Math.sqrt(x * x + y * y + z * z)
				- SensorManager.STANDARD_GRAVITY;

		write(new SensorPacket(timestamp, x, y, z, m, temperature, magfield,
				orientation));
	}

	/**
	 * This method gets called automatically whenever a sensor's accuracy has
	 * changed. We currently ignore this event.
	 * 
	 * @param sensor
	 *            The SensorEvent detailing the change in sensor data.
	 * @param accuracy
	 *            The new accuracy.
	 * 
	 * 
	 */
	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
	}

	/**
	 * Registers the appropriate listeners using the SensorManager.
	 */
	@Override
	protected void onPluginStart() {
		loggingSpeed = Integer.parseInt(prefs.getString(
				SENSOR_LOGGER_INTERVAL_PREF, SENSOR_LOGGER_DEFAULT_INTERVAL));
		calculateOrientation = prefs.getBoolean(
				SENSOR_LOGGER_CALCULATE_ORIENTATION, false);
		pluginEnabled = prefs.getBoolean(SENSOR_LOGGER_ENABLE_PREF, false);
		if (!pluginEnabled) {
			return;
		}
		Log.i(PLUGIN_NAME, "Registered Sensor Listener");
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), loggingSpeed);
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE),
				SensorManager.SENSOR_DELAY_UI);
		logging = true;
	}

	/**
	 * Unregisters the appropriate listeners using the SensorManager.
	 */
	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		Log.i(PLUGIN_NAME, "Unregistered Sensor Listener.");
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE));
		logging = false;
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		boolean changed = false;
		final int newLoggingSpeed = Integer.parseInt(prefs.getString(
				SENSOR_LOGGER_INTERVAL_PREF, SENSOR_LOGGER_DEFAULT_INTERVAL));
		if (newLoggingSpeed != loggingSpeed) {
			changed = true;
		}
		final boolean newCalculateOrientation = prefs.getBoolean(
				SENSOR_LOGGER_CALCULATE_ORIENTATION, false);
		if (!newCalculateOrientation) {
			orientation[0] = 0.0f;
			orientation[1] = 0.0f;
			orientation[2] = 0.0f;
		}
		if (newCalculateOrientation != calculateOrientation) {
			changed = true;
		}
		final boolean pluginEnabledNew = prefs.getBoolean(
				SENSOR_LOGGER_ENABLE_PREF, false);
		// If something changed but the enabled status didn't, then we need to
		// restart the plugin.
		if (changed && pluginEnabled && pluginEnabledNew) {
			onPluginStop();
			onPluginStart();
		}
		super.changePluginEnabledStatus(pluginEnabledNew);
	}

	/**
	 * This method gets called automatically whenever a sensor has changed.
	 * 
	 * @param event
	 *            the SensorEvent detailing the change in sensor data.
	 */
	public void onSensorChanged(final SensorEvent event) {
		if (logging) {
			final Sensor sensor = event.sensor;
			final int type = sensor.getType();
			switch (type) {
			case Sensor.TYPE_MAGNETIC_FIELD:
				magfield = event.values.clone();
				magfieldUpdated = true;
				break;
			case Sensor.TYPE_TEMPERATURE:
				temperature = event.values[0];
				break;
			case Sensor.TYPE_ACCELEROMETER:
				if (calculateOrientation && magfieldUpdated) {
					magfieldUpdated = false;
					final int matrix_size = 16;
					final float[] R = new float[matrix_size];
					final float[] I = new float[matrix_size];
					final float[] outR = new float[matrix_size];

					SensorManager.getRotationMatrix(R, I, event.values,
							magfield);
					SensorManager.remapCoordinateSystem(R,
							SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
					SensorManager.getOrientation(outR, orientation);
				}
				/*
				 * The event.timestamp gives the timestamp in nanoseconds since
				 * the device was booted, so we need to do some math to
				 * translate this into system time.
				 */
				if (timestamp_offset == 0) {
					final long millis = event.timestamp / 1000000;
					timestamp_offset = System.currentTimeMillis() - millis;
				}

				logAccelerometerData(event.values, event.timestamp / 1000000
						+ timestamp_offset);
			}
		}
	}
}
