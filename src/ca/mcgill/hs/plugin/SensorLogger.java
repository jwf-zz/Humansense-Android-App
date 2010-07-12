package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * An InputPlugin which gets data from the phone's available sensors.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class SensorLogger extends InputPlugin implements SensorEventListener {

	// Boolean ON-OFF switch *Temporary only*
	private boolean PLUGIN_ACTIVE;

	// The SensorManager used to register listeners.
	private final SensorManager sensorManager;

	// A boolean checking whether or not we are logging at a given moment.
	private static boolean logging = false;

	// The speed at which the accelerometer will log.
	private static int loggingSpeed;

	// Offset for timestamps
	private static long timestamp_offset = 0;

	// Variables used to write out the sensor data received.
	private static float temperature = 0.0f;
	private static float[] magfield = { 0.0f, 0.0f, 0.0f };
	private static boolean magfieldUpdated = false;
	private static float[] orientation = { 0.0f, 0.0f, 0.0f };

	// The Context used for the preferences.
	private final Context context;

	/**
	 * This is the basic constructor for the SensorLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a SensorManager.
	 * 
	 * @param gpsm
	 * @param context
	 */
	public SensorLogger(final SensorManager sensorManager, final Context context) {
		this.sensorManager = sensorManager;
		this.context = context;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		loggingSpeed = Integer.parseInt(prefs.getString(
				"sensorIntervalPreference", "0"));

		PLUGIN_ACTIVE = prefs.getBoolean("sensorLoggerEnable", false);
	}

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[2];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				"sensorLoggerEnable", "Sensor Plugin",
				"Enables or disables this plugin.", "SensorLogger is on.",
				"SensorLogger is off.");

		prefs[1] = PreferenceFactory.getListPreference(c,
				R.array.sensorLoggerIntervalStrings,
				R.array.sensorLoggerIntervalValues, "0",
				"sensorIntervalPreference",
				R.string.sensorlogger_interval_pref,
				R.string.sensorlogger_interval_pref_summary);

		return prefs;
	}

	/**
	 * Returns whether or not this InputPlugin has Preferences.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
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

		write(new SensorLoggerPacket(timestamp, x, y, z, m, temperature,
				magfield, orientation));
	}

	/**
	 * This method gets called automatically whenever a sensor's accuracy has
	 * changed.
	 * 
	 * @param event
	 *            the SensorEvent detailing the change in sensor data.
	 * @param accuracy
	 *            the new accuracy.
	 * 
	 * @override
	 */
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean(
				"sensorLoggerEnable", false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}
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
				if (magfieldUpdated) {
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

	/**
	 * Registers the appropriate listeners using the SensorManager.
	 */
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		Log.i("SensorLogger", "Registered Sensor Listener");
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
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		Log.i("SensorLogger", "Unregistered Sensor Listener.");
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		sensorManager.unregisterListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE));
		logging = false;
	}

	public static class SensorLoggerPacket implements DataPacket {

		final long time;
		final float x;
		final float y;
		final float z;
		final float m;
		final float temperature;
		final float[] magfield;
		final float[] orientation;
		final static String PLUGIN_NAME = "SensorLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

		public SensorLoggerPacket(final long time, final float x,
				final float y, final float z, final float m,
				final float temperature, final float[] magfield,
				final float[] orientation) {
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
			return new SensorLoggerPacket(time, x, y, z, m, temperature,
					magfield, orientation);
		}

		@Override
		public int getDataPacketId() {
			return SensorLoggerPacket.PLUGIN_ID;
		}

		@Override
		public String getInputPluginName() {
			return SensorLoggerPacket.PLUGIN_NAME;
		}

	}

}
