package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
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

		@Override
		public void onLocationChanged(final Location location) {
			logLocation(location);
		}

		@Override
		public void onProviderDisabled(final String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
			available = false;
		}

		@Override
		public void onProviderEnabled(final String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
		}

		@Override
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

	private static final String GPS_LOGGER_ENABLE_PREF = "gpsLoggerEnable";
	private static final String GPS_LOGGER_DISTANCE_PREF = "gpsLoggerDistancePreference";
	private static final String GPS_LOGGER_DISTANCE_DEFAULT = "0";
	private static final String GPS_LOGGER_INTERVAL_PREF = "gpsLoggerIntervalPreference";
	private static final String GPS_LOGGER_INTERVAL_DEFAULT = "30000";
	private static final String GPS_LOGGER_TIMEOUT_PREF = "gpsLoggerTimeoutPreference";
	private static final String GPS_LOGGER_TIMEOUT_DEFAULT = "-1";

	private static Handler gpsTimeoutHandler = new Handler();

	/**
	 * This keeps track of whether another plugin has remotely disabled
	 * scanning. For example, the location clustering plugin can disable the GPS
	 * plugin if the user is determined to be stationary.
	 */
	private static Boolean remotelyDisabled = false;

	/**
	 * This keeps track of whether another plugin has requested that this plugin
	 * be disabled. We use this so that a plugin can request that this plugin be
	 * disabled after the next scan completes, rather than having other plugins
	 * disable the plugin immediately.
	 */
	private static Boolean pendingRemoteDisable = false;

	private static boolean listeningForLocationUpdates = false;

	private static int gpsTimeoutInMillis = -1;

	protected static Runnable checkIfTimedOut = new Runnable() {
		@Override
		public void run() {
			final Location lastLocation = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (listeningForLocationUpdates) {
				if (lastLocation != null) {
					// We have a previous location, just log that.
					((GPSLogger) PluginFactory.getInputPlugin(GPSLogger.class))
							.logLocation(lastLocation);
				}
				// Location update has timed out.
				stopListeningForLocationUpdates();
				if (pendingRemoteDisable) {
					remotelyDisabled = true;
					pendingRemoteDisable = false;
				}
				if (!remotelyDisabled) {
					// Start up again in minUpdateFrequency milliseconds.
					Log.d(PLUGIN_NAME,
							"GPS Has timed out, disabling and sleeping for a bit.");
					gpsTimeoutHandler.postDelayed(restartGPSUpdates,
							minUpdateFrequency);
				}
			}
		}
	};

	protected static Runnable restartGPSUpdates = new Runnable() {
		@Override
		public void run() {
			if (pendingRemoteDisable) {
				remotelyDisabled = true;
				pendingRemoteDisable = false;
			}
			if (!remotelyDisabled) {
				Log.d(PLUGIN_NAME, "Restarting GPS updates after timeout.");
				if (pluginEnabled && !remotelyDisabled) {
					startListeningForLocationUpdates();
				}
			}
		}
	};

	// Keeps track of whether this plugin is enabled or not.
	private static boolean pluginEnabled;

	// The LocationManager used to request location updates.
	private static LocationManager locationManager = null;

	// A GPSLocationListener which listens for location updates.
	private static GPSLocationListener locationListener = null;

	// The minimum distance the person has to travel for an update to be valid.
	private static int minUpdateDistance;

	// The minimum amount of time, in milliseconds, that has to pass between two
	// subsequent updates
	// for an update to be valid.
	private static int minUpdateFrequency;

	public final static String PLUGIN_NAME = "GPSLogger";

	public final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	public static void disableAfterNextScan() {
		Log.d(PLUGIN_NAME,
				"We have been asked to stop scanning after the next scan completes.");
		pendingRemoteDisable = true;
	}

	public static void enable() {
		Log.d(PLUGIN_NAME, "We have been remotely enabled.");
		startListeningForLocationUpdates();
		remotelyDisabled = false;

		// Reset this so that a pending disable will not occur.
		pendingRemoteDisable = false;
	}

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param activity
	 *            The PreferenceActivity that will display the preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[4];

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

		prefs[2] = PreferenceFactory.getListPreference(activity,
				R.array.gpslogger_pref_timeout_strings,
				R.array.gpslogger_pref_timeout_values,
				GPS_LOGGER_TIMEOUT_DEFAULT, GPS_LOGGER_TIMEOUT_PREF,
				R.string.gpslogger_timeout_pref,
				R.string.gpslogger_timeout_pref_summary);

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

	private static void removeAllTimers() {
		gpsTimeoutHandler.removeCallbacks(checkIfTimedOut);
		gpsTimeoutHandler.removeCallbacks(restartGPSUpdates);
	}

	/**
	 * Start listening for location updates.
	 */
	protected static void startListeningForLocationUpdates() {
		removeAllTimers();
		if (!listeningForLocationUpdates && locationManager != null) {
			locationManager
					.requestLocationUpdates(LocationManager.GPS_PROVIDER,
							minUpdateFrequency, minUpdateDistance,
							locationListener, Looper.getMainLooper());
			listeningForLocationUpdates = true;
			if (gpsTimeoutInMillis > 0) {
				gpsTimeoutHandler.postDelayed(checkIfTimedOut,
						gpsTimeoutInMillis);
			}
			Log.i(PLUGIN_NAME, "Registered Location Listener.");
		}
	}

	/**
	 * Stops listening for location updates.
	 */
	protected static void stopListeningForLocationUpdates() {
		removeAllTimers();
		if (listeningForLocationUpdates && locationManager != null) {
			locationManager.removeUpdates(locationListener);
			listeningForLocationUpdates = false;
			Log.i(PLUGIN_NAME, "Unregistered Location Listener.");
		}
	}

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
	 * Writes out the data to the plugin outputstream.
	 * 
	 * @param location
	 *            the current Location.
	 */
	protected void logLocation(final Location location) {
		Log.i(PLUGIN_NAME, "GPS Data received.");
		if (pendingRemoteDisable) {
			synchronized (pendingRemoteDisable) {
				if (pendingRemoteDisable) {
					Log.d(PLUGIN_NAME, "Disabling because of pending request.");
					pendingRemoteDisable = false;
					stopListeningForLocationUpdates();
					remotelyDisabled = true;
				}
			}
		}
		write(new GPSPacket(location.getTime(), location.getAccuracy(),
				location.getBearing(), location.getSpeed(),
				location.getAltitude(), location.getLatitude(),
				location.getLongitude()));
	}

	/**
	 * This method requests location updates from the LocationManager.
	 */
	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(GPS_LOGGER_ENABLE_PREF, false);
		minUpdateDistance = Integer.parseInt(prefs.getString(
				GPS_LOGGER_DISTANCE_PREF, GPS_LOGGER_DISTANCE_DEFAULT));
		minUpdateFrequency = Integer.parseInt(prefs.getString(
				GPS_LOGGER_INTERVAL_PREF, GPS_LOGGER_INTERVAL_DEFAULT));
		gpsTimeoutInMillis = Integer.parseInt(prefs.getString(
				GPS_LOGGER_TIMEOUT_PREF, GPS_LOGGER_TIMEOUT_DEFAULT));
		if (!pluginEnabled) {
			return;
		}
		startListeningForLocationUpdates();
	}

	/**
	 * This method removes the requested location updates.
	 */
	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		stopListeningForLocationUpdates();
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
			stopListeningForLocationUpdates();
			minUpdateDistance = Integer.parseInt(prefs.getString(
					GPS_LOGGER_DISTANCE_PREF, GPS_LOGGER_DISTANCE_DEFAULT));
			minUpdateFrequency = Integer.parseInt(prefs.getString(
					GPS_LOGGER_INTERVAL_PREF, GPS_LOGGER_INTERVAL_DEFAULT));
			gpsTimeoutInMillis = Integer.parseInt(prefs.getString(
					GPS_LOGGER_TIMEOUT_PREF, GPS_LOGGER_TIMEOUT_DEFAULT));
			startListeningForLocationUpdates();
		}
	}

}
