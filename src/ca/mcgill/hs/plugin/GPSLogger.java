package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * An InputPlugin which gets data from the available GPS signals around.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class GPSLogger extends InputPlugin {

	private class GPSLocationListener implements
			android.location.LocationListener {

		private static final String TAG = "GPSLocationLogger";

		@SuppressWarnings("unused")
		private final LocationManager lm;

		@SuppressWarnings("unused")
		private boolean available = false;

		public GPSLocationListener(final LocationManager lm) {
			this.lm = lm;
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

	public static class GPSLoggerPacket implements DataPacket {

		final long time;
		final float accuracy;
		final float bearing;
		final float speed;
		final double altitude;
		final double latitude;
		final double longitude;
		final static String PLUGIN_NAME = "GPSLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

		public GPSLoggerPacket(final long time, final float accuracy,
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
			return new GPSLoggerPacket(time, accuracy, bearing, speed,
					altitude, latitude, longitude);
		}

		@Override
		public int getDataPacketId() {
			return GPSLoggerPacket.PLUGIN_ID;
		}

		@Override
		public String getInputPluginName() {
			return GPSLoggerPacket.PLUGIN_NAME;
		}

	}

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[3];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				"gpsLoggerEnable", "GPS Plugin",
				"Enables or disables this plugin.", "GPSLogger is on.",
				"GPSLogger is off.");

		prefs[1] = PreferenceFactory.getListPreference(c,
				R.array.gpsLoggerIntervalStrings,
				R.array.gpsLoggerIntervalValues, "30000",
				"gpsLoggerIntervalPreference",
				R.string.gpslogger_interval_pref,
				R.string.gpslogger_interval_pref_summary);

		prefs[2] = PreferenceFactory.getListPreference(c,
				R.array.gpsLoggerDistanceStrings,
				R.array.gpsLoggerDistanceValues, "0",
				"gpsLoggerDistancePreference",
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

	// Boolean ON-OFF switch *Temporary only*
	private boolean PLUGIN_ACTIVE;

	// The LocationManager used to request location updates.
	private final LocationManager gpsm;

	// A GPSLocationListener which listens for location updates.
	private final GPSLocationListener gpsll;

	// The minimum distance the person has to travel for an update to be valid.
	private int MIN_DIST;

	// The minimum amount of time, in milliseconds, that has to pass between two
	// subsequent updates
	// for an update to be valid.
	private int UPDATE_FREQ;

	// The Context for the preferences.
	private final Context context;

	/**
	 * This is the basic constructor for the GPSLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a LocationManager and a Context.
	 * 
	 * @param gpsm
	 *            the LocationManager used for this plugin.
	 * @param context
	 *            needed for the Preference objects.
	 */
	public GPSLogger(final LocationManager gpsm, final Context context) {

		this.gpsm = gpsm;
		gpsll = new GPSLocationListener(gpsm);

		this.context = context;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		PLUGIN_ACTIVE = prefs.getBoolean("gpsLoggerEnable", false);
		MIN_DIST = Integer.parseInt(prefs.getString(
				"gpsLoggerDistancePreference", "0"));
		UPDATE_FREQ = Integer.parseInt(prefs.getString(
				"gpsLoggerIntervalPreference", "30000"));
	}

	/**
	 * Creates a GPSLocationPacket with the current location's coordinates.
	 * 
	 * @param loc
	 *            the current Location.
	 */
	private void getNewLocation(final Location loc) {
		Log.i("GPSLocationLogger local", "Data received.");
		write(new GPSLoggerPacket(loc.getTime(), loc.getAccuracy(), loc
				.getBearing(), loc.getSpeed(), loc.getAltitude(), loc
				.getLatitude(), loc.getLongitude()));
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean("gpsLoggerEnable",
				false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}

		if (PLUGIN_ACTIVE) {
			gpsm.removeUpdates(gpsll);
			MIN_DIST = Integer.parseInt(prefs.getString(
					"gpsLoggerDistancePreference", "0"));
			UPDATE_FREQ = Integer.parseInt(prefs.getString(
					"gpsLoggerIntervalPreference", "30000"));
			gpsm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					UPDATE_FREQ, MIN_DIST, gpsll, Looper.getMainLooper());
		}
	}

	/**
	 * This method requests location updates from the LocationManager.
	 * 
	 * @Override
	 */
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		gpsm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_FREQ,
				MIN_DIST, gpsll, Looper.getMainLooper());
		Log.i("GPSLocationLogger local", "Registered Location Listener.");
	}

	/**
	 * This method removes the requested location updates.
	 * 
	 * @Override
	 */
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		gpsm.removeUpdates(gpsll);
		Log.i("GPSLocationLogger local", "Unregistered Location Listener.");
	}

}
