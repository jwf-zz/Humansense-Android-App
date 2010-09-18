package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * An InputPlugin which gets data from the GPS receiver.
 */
public final class GPSLogger extends InputPlugin {

	private final class GPSLocationListener implements
			android.location.LocationListener {

		private static final String TAG = "GPSLocationLogger";

		@SuppressWarnings("unused")
		private final LocationManager locationmanager;

		@SuppressWarnings("unused")
		private boolean available = false;

		public GPSLocationListener(final LocationManager locationManager) {
			this.locationmanager = locationManager;
		}

		public void onLocationChanged(final Location location) {
			getNewLocation(location);
		}

		public void onProviderDisabled(final String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
			available = false;
		}

		public void onProviderEnabled(final String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			if (status == LocationProvider.AVAILABLE) {
				Log.d(TAG, "GPS Is Available.");
				available = true;
			} else {
				Log.d(TAG, "GPS Is Not Available.");
				available = false;
			}
		}
	}

	public final static class GPSPacket implements DataPacket {

		final long time;
		final float accuracy;
		final float bearing;
		final float speed;
		final double altitude;
		final double latitude;
		final double longitude;
		final static String PACKET_NAME = "GPSPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

		public GPSPacket(final long time, final float accuracy,
				final float bearing, final float speed, final double altitude,
				final double latitude, final double longitude) {
			this.time = time;
			this.accuracy = accuracy;
			this.bearing = bearing;
			this.speed = speed;
			this.altitude = altitude;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public DataPacket clone() {
			return new GPSPacket(time, accuracy, bearing, speed, altitude,
					latitude, longitude);
		}

		@Override
		public int getDataPacketId() {
			return GPSPacket.PACKET_ID;
		}

		@Override
		public String getInputPluginName() {
			return GPSPacket.PACKET_NAME;
		}

	}

	private static final String GPS_LOGGER_DISTANCE_DEFAULT = "0";
	private static final String GPS_LOGGER_INTERVAL_DEFAULT = "30000";
	private static final String GPS_LOGGER_INTERVAL_PREF = "gpsLoggerIntervalPreference";
	private static final String GPS_LOGGER_DISTANCE_PREF = "gpsLoggerDistancePreference";
	private static final String GPS_LOGGER_ENABLE_PREF = "gpsLoggerEnable";

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[3];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				GPS_LOGGER_ENABLE_PREF, R.string.gpslogger_enable_pref_label,
				R.string.gpslogger_enable_pref_summary,
				R.string.gpslogger_enable_pref_on,
				R.string.gpslogger_enable_pref_off);

		prefs[1] = PreferenceFactory.getListPreference(activity,
				R.array.bluetoothlogger_pref_interval_strings,
				R.array.gpslogger_pref_interval_values,
				GPS_LOGGER_INTERVAL_DEFAULT, GPS_LOGGER_INTERVAL_PREF,
				R.string.gpslogger_interval_pref,
				R.string.gpslogger_interval_pref_summary);

		prefs[2] = PreferenceFactory.getListPreference(activity,
				R.array.gpslogger_pref_distance_strings,
				R.array.gpslogger_pref_distance_values,
				GPS_LOGGER_DISTANCE_DEFAULT, GPS_LOGGER_DISTANCE_PREF,
				R.string.gpslogger_distance_pref,
				R.string.gpslogger_distance_pref_summary);

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

	// Keeps track of whether this plugin is enabled or not.
	private boolean pluginEnabled;

	// The LocationManager used to request location updates.
	private final LocationManager locationManager;

	// A GPSLocationListener which listens for location updates.
	private final GPSLocationListener locationListener;

	// The minimum distance the person has to travel for an update to be valid.
	private int minUpdateDistance;

	// The minimum amount of time, in milliseconds, that has to pass between two
	// subsequent updates
	// for an update to be valid.
	private int minUpdateFrequency;

	public final static String PLUGIN_NAME = "GPSLogger";

	public final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	final private SharedPreferences prefs;

	/**
	 * This is the basic constructor for the GPSLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a LocationManager and a Context.
	 * 
	 * @param context
	 *            needed for the Preference objects.
	 */
	public GPSLogger(final Context context) {

		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new GPSLocationListener(locationManager);

		prefs = PreferenceFactory.getSharedPreferences();
	}

	/**
	 * Creates a GPSLocationPacket with the current location's coordinates.
	 * 
	 * @param location
	 *            the current Location.
	 */
	private void getNewLocation(final Location location) {
		Log.i(PLUGIN_NAME, "GPS Data received.");
		write(new GPSPacket(location.getTime(), location.getAccuracy(),
				location.getBearing(), location.getSpeed(), location
						.getAltitude(), location.getLatitude(), location
						.getLongitude()));
	}

	/**
	 * This method requests location updates from the LocationManager.
	 * 
	 * @Override
	 */
	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(GPS_LOGGER_ENABLE_PREF, false);
		minUpdateDistance = Integer.parseInt(prefs.getString(
				GPS_LOGGER_DISTANCE_PREF, GPS_LOGGER_DISTANCE_DEFAULT));
		minUpdateFrequency = Integer.parseInt(prefs.getString(
				GPS_LOGGER_INTERVAL_PREF, GPS_LOGGER_INTERVAL_DEFAULT));
		if (!pluginEnabled) {
			return;
		}
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				minUpdateFrequency, minUpdateDistance, locationListener, Looper
						.getMainLooper());
		Log.i(PLUGIN_NAME, "Registered Location Listener.");
	}

	/**
	 * This method removes the requested location updates.
	 * 
	 * @Override
	 */
	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		locationManager.removeUpdates(locationListener);
		Log.i(PLUGIN_NAME, "Unregistered Location Listener.");
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				GPS_LOGGER_ENABLE_PREF, false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		}

		if (pluginEnabled) {
			locationManager.removeUpdates(locationListener);
			minUpdateDistance = Integer.parseInt(prefs.getString(
					GPS_LOGGER_DISTANCE_PREF, GPS_LOGGER_DISTANCE_DEFAULT));
			minUpdateFrequency = Integer.parseInt(prefs.getString(
					GPS_LOGGER_INTERVAL_PREF, GPS_LOGGER_INTERVAL_DEFAULT));
			locationManager
					.requestLocationUpdates(LocationManager.GPS_PROVIDER,
							minUpdateFrequency, minUpdateDistance,
							locationListener, Looper.getMainLooper());
		}
	}

}
