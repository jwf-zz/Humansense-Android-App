/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
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
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

/**
 * An InputPlugin which gets data from the GPS receiver.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public final class GPSLogger extends InputPlugin {

	/**
	 * Listener for obtaining data from the GPS receiver.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	private final class GPSLocationListener implements
			android.location.LocationListener {

		private static final String TAG = "GPSLocationLogger";

		@SuppressWarnings("unused")
		private final LocationManager locationmanager;

		@SuppressWarnings("unused")
		private boolean available = false;

		/**
		 * Constructs a new location listener from a {@link LocationManager}
		 * 
		 * @param locationManager
		 *            The {@link LocationManager} that handles the GPS receiver.
		 */
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

	/**
	 * Packet with data from the GPS receiver.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
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
			return GPSLogger.PLUGIN_NAME;
		}

	}

	private static final String GPS_LOGGER_ENABLE_PREF = "gpsLoggerEnable";
	private static final String GPS_LOGGER_DISTANCE_PREF = "gpsLoggerDistancePreference";
	private static final String GPS_LOGGER_DISTANCE_DEFAULT = "0";
	private static final String GPS_LOGGER_INTERVAL_PREF = "gpsLoggerIntervalPreference";
	private static final String GPS_LOGGER_INTERVAL_DEFAULT = "30000";
	private static final String GPS_LOGGER_TIMEOUT_PREF = "gpsLoggerTimeoutPreference";
	private static final String GPS_LOGGER_TIMEOUT_DEFAULT = "10000";

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

	private static int gpsTimeoutInMillis = 10000;

	/**
	 * Used to check if a request has timed out and act appropriately.
	 */
	protected Runnable checkIfTimedOut = new Runnable() {
		@Override
		public void run() {
			if (listeningForLocationUpdates) {
				final Location lastLocation = locationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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

	/**
	 * Used to restart the GPS receiver after a specific amount of time.
	 */
	protected Runnable restartGPSUpdates = new Runnable() {
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

	/**
	 * @see InputPlugin#getPreferences(PreferenceActivity)
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[4];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				GPS_LOGGER_ENABLE_PREF, R.string.gpslogger_enable_pref_label,
				R.string.gpslogger_enable_pref_summary,
				R.string.gpslogger_enable_pref_on,
				R.string.gpslogger_enable_pref_off, true);

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

		prefs[3] = PreferenceFactory.getListPreference(activity,
				R.array.gpslogger_pref_timeout_strings,
				R.array.gpslogger_pref_timeout_values,
				GPS_LOGGER_TIMEOUT_DEFAULT, GPS_LOGGER_TIMEOUT_PREF,
				R.string.gpslogger_timeout_pref,
				R.string.gpslogger_timeout_pref_summary);

		return prefs;
	}

	/**
	 * @see InputPlugin#hasPreferences()
	 */
	public static boolean hasPreferences() {
		return true;
	}

	final private SharedPreferences prefs;

	/**
	 * This is the basic constructor for the GPSLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a LocationManager and a Context.
	 * 
	 * @param context
	 *            The application context, required to access the preferences.
	 */
	public GPSLogger(final Context context) {

		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new GPSLocationListener(locationManager);

		prefs = PreferenceFactory.getSharedPreferences(context);
	}

	/**
	 * Request that the plugin disable the GPS receiver once the next request
	 * has completed.
	 */
	public void disableAfterNextScan() {
		Log.d(PLUGIN_NAME,
				"We have been asked to stop scanning after the next scan completes.");
		pendingRemoteDisable = true;
	}

	/**
	 * Enable the plugin. We have a special public method for this in order to
	 * allow other plugins to manage the state of the GPS receiver. For example,
	 * the {@link LocationClusterer} plugin can disable the GPS to save power
	 * when it is determined that the user is stationary.
	 */
	public void enable() {
		Log.d(PLUGIN_NAME, "We have been remotely enabled.");
		startListeningForLocationUpdates();
		remotelyDisabled = false;

		// Reset this so that a pending disable will not occur.
		pendingRemoteDisable = false;
	}

	/**
	 * Parse the current location and write a GPSPacket to the plugin
	 * outputstream.
	 * 
	 * @param location
	 *            The current location.
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

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(GPS_LOGGER_ENABLE_PREF, true);
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

	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		stopListeningForLocationUpdates();
	}

	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				GPS_LOGGER_ENABLE_PREF, true);
		if (pluginEnabled && pluginEnabledNew) {
			stopListeningForLocationUpdates();
			minUpdateDistance = Integer.parseInt(prefs.getString(
					GPS_LOGGER_DISTANCE_PREF, GPS_LOGGER_DISTANCE_DEFAULT));
			minUpdateFrequency = Integer.parseInt(prefs.getString(
					GPS_LOGGER_INTERVAL_PREF, GPS_LOGGER_INTERVAL_DEFAULT));
			gpsTimeoutInMillis = Integer.parseInt(prefs.getString(
					GPS_LOGGER_TIMEOUT_PREF, GPS_LOGGER_TIMEOUT_DEFAULT));
			startListeningForLocationUpdates();
		}
		super.changePluginEnabledStatus(pluginEnabledNew);
	}

	/**
	 * Stops any existing timers.
	 */
	private void removeAllTimers() {
		gpsTimeoutHandler.removeCallbacks(checkIfTimedOut);
		gpsTimeoutHandler.removeCallbacks(restartGPSUpdates);
	}

	/**
	 * Start listening for location updates.
	 */
	protected void startListeningForLocationUpdates() {
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
	protected void stopListeningForLocationUpdates() {
		removeAllTimers();
		if (listeningForLocationUpdates && locationManager != null) {
			if (locationListener != null) {
				locationManager.removeUpdates(locationListener);
			}
			listeningForLocationUpdates = false;
			Log.i(PLUGIN_NAME, "Unregistered Location Listener.");
		}
	}
}
