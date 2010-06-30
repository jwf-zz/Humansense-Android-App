package ca.mcgill.hs.plugin;

import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;

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

/**
 * An InputPlugin which gets data from the available GPS signals around.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class GPSLogger extends InputPlugin{
	
	//Boolean ON-OFF switch *Temporary only*
	private final boolean PLUGIN_ACTIVE;
	
	//The LocationManager used to request location updates.
	private final LocationManager gpsm;
	
	//A GPSLocationListener which listens for location updates.
	private final GPSLocationListener gpsll;
	
	//The minimum distance the person has to travel for an update to be valid.
	private int MIN_DIST;
	
	//The minimum amount of time, in milliseconds, that has to pass between two subsequent updates
	//for an update to be valid.
	private int UPDATE_FREQ;
	
	/**
	 * This is the basic constructor for the GPSLogger plugin. It has to be instantiated
	 * before it is started, and needs to be passed a reference to a LocationManager and a Context.
	 * 
	 * @param gpsm the LocationManager used for this plugin.
	 * @param context needed for the Preference objects.
	 */
	public GPSLogger(LocationManager gpsm, Context context){

		this.gpsm = gpsm;
		gpsll = new GPSLocationListener(gpsm);
		
		SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(context);		
		PLUGIN_ACTIVE = prefs.getBoolean("gpsLoggerEnable", false);
		MIN_DIST = Integer.parseInt(prefs.getString("gpsLoggerDistancePreference", "0"));
		UPDATE_FREQ = Integer.parseInt(prefs.getString("gpsLoggerIntervalPreference", "30000"));
	}
	
	/**
	 * This method requests location updates from the LocationManager.
	 *
	 * @Override
	 */
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) return;
		gpsm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				UPDATE_FREQ,
				MIN_DIST, 
				gpsll, Looper.getMainLooper());
		Log.i("GPSLocationLogger local", "Registered Location Listener.");
	}


	/**
	 * This method removes the requested location updates.
	 * 
	 * @Override
	 */
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) return;
		gpsm.removeUpdates(gpsll);
		Log.i("GPSLocationLogger local", "Unregistered Location Listener.");
	}
	
	/**
	 * Creates a GPSLocationPacket with the current location's coordinates.
	 * 
	 * @param loc the current Location.
	 */
	private void getNewLocation(Location loc){
		Log.i("GPSLocationLogger local", "Data received.");
		write(new GPSLoggerPacket(
				loc.getTime(), loc.getAccuracy(), loc.getBearing(), loc.getSpeed(),
				loc.getAltitude(), loc.getLatitude(), loc.getLongitude()));
	}
	
	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 * 
	 * @override
	 */
	public static Preference[] getPreferences(Context c){
		Preference[] prefs = new Preference[3];
		
		prefs[0] = PreferenceFactory.getCheckBoxPreference(c, "gpsLoggerEnable",
				"GPS Plugin", "Enables or disables this plugin.",
				"GPSLogger is on.", "GPSLogger is off.");
		
		prefs[1] = PreferenceFactory.getListPreference(c, R.array.gpsLoggerIntervalStrings,
				R.array.gpsLoggerIntervalValues, "30000", "gpsLoggerIntervalPreference",
				R.string.gpslogger_interval_pref, R.string.gpslogger_interval_pref_summary);
		
		prefs [2] = PreferenceFactory.getListPreference(c, R.array.gpsLoggerDistanceStrings,
				R.array.gpsLoggerDistanceValues, "0", "gpsLoggerDistancePreference",
				R.string.gpslogger_distance_pref, R.string.gpslogger_distance_pref_summary);
		
		return prefs;
	}
	
	/**
	 * Returns whether or not this InputPlugin has Preferences.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences(){return true;}
	
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- GPSLocationListener
	// ***********************************************************************************
	
	private class GPSLocationListener implements android.location.LocationListener {

		private static final String TAG = "GPSLocationLogger";
		private final LocationManager lm;
		private boolean available = false;

		public GPSLocationListener(LocationManager lm) {
			this.lm = lm;
		}
		
		public void onLocationChanged(Location location) {
			getNewLocation(location);
		}

		public void onProviderDisabled(String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
			available = false;
		}

		public void onProviderEnabled(String provider) {
			Log.d(TAG, "GPS onProviderEnabled called.");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == LocationProvider.AVAILABLE) {
				Log.d(TAG,"GPS Is Available.");
				available = true;
			}
			else {
				Log.d(TAG,"GPS Is Not Available.");
				available = false;
			}
		}
	}
	
	// ***********************************************************************************
	// PUBLIC INNER CLASS -- GPSLocationPacket
	// ***********************************************************************************
	
	public static class GPSLoggerPacket implements DataPacket{
		
		final long time;
		final float accuracy;
		final float bearing;
		final float speed;
		final double altitude;
		final double latitude;
		final double longitude;
		final static String PLUGIN_NAME = "GPSLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();
		
		public GPSLoggerPacket(long time, float accuracy, float bearing, float speed, double altitude, double latitude, double longitude){
			this.time = time;
			this.accuracy = accuracy;
			this.bearing = bearing;
			this.speed = speed;
			this.altitude = altitude;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public DataPacket clone(){
			return new GPSLoggerPacket(time, accuracy, bearing, speed, altitude, latitude, longitude);
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

}
