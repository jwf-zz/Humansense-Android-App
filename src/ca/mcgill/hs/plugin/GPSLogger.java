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
import android.preference.ListPreference;
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
	 * This is the basic constructor for the WifiLogger plugin. It has to be instantiated
	 * before it is started, and needs to be passed a reference to a WifiManager, a Context
	 * and a WritableByteChannel (java.nio).
	 * 
	 * @param gpsm
	 * @param context
	 */
	public GPSLogger(LocationManager gpsm, Context context){
		this.gpsm = gpsm;
		gpsll = new GPSLocationListener(gpsm);
		
		SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(context);
		try {
			MIN_DIST = Integer.parseInt(prefs.getString("gpsLoggerDistancePreference", "0"));
			UPDATE_FREQ = Integer.parseInt(prefs.getString("gpsLoggerIntervalPreference", "30000"));
		} catch (NumberFormatException defVals) {
			MIN_DIST = 0;
			UPDATE_FREQ = 30000;
			Log.e("GPSLogger - PreferenceError", "Unable to get one or more preferences for this plugin.");
		}
	}

	@Override
	public void startPlugin() {
		gpsm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				UPDATE_FREQ,
				MIN_DIST, 
				gpsll, Looper.getMainLooper());
		Log.i("GPSLocationLogger local", "Registered Location Listener.");
	}

	@Override
	public void stopPlugin() {
		gpsm.removeUpdates(gpsll);
		Log.i("GPSLocationLogger local", "Unregistered Location Listener.");
	}
	
	/**
	 * Creates a GPSLocationPacket with the current location's coordinates.
	 * @param loc the current Location.
	 */
	private void getNewLocation(Location loc){
		Log.i("GPSLocationLogger local", "Data received.");
		write(new GPSLocationPacket(
				loc.getTime(), loc.getAccuracy(), loc.getBearing(), loc.getSpeed(),
				loc.getAltitude(), loc.getLatitude(), loc.getLongitude()));
	}
	
	public static Preference[] getPreferences(Context c){
		Preference[] prefs = new Preference[2];
		
		prefs[0] = PreferenceFactory.getListPreference(c, R.array.gpsLoggerIntervalStrings,
				R.array.gpsLoggerIntervalValues, "30000", "gpsLoggerIntervalPreference",
				R.string.gpslogger_interval_pref, R.string.gpslogger_interval_pref_summary);
		
		prefs [1] = PreferenceFactory.getListPreference(c, R.array.gpsLoggerDistanceStrings,
				R.array.gpsLoggerDistanceValues, "0", "gpsLoggerDistancePreference",
				R.string.gpslogger_distance_pref, R.string.gpslogger_distance_pref_summary);
		
		return prefs;
	}
	
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
	
	public class GPSLocationPacket implements DataPacket{
		
		final long time;
		final float accuracy;
		final float bearing;
		final float speed;
		final double altitude;
		final double latitude;
		final double longitude;
		
		public GPSLocationPacket(long time, float accuracy, float bearing, float speed, double altitude, double latitude, double longitude){
			this.time = time;
			this.accuracy = accuracy;
			this.bearing = bearing;
			this.speed = speed;
			this.altitude = altitude;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public String getInputPluginName() {
			return "GPSLocationLogger";
		}
		
		public DataPacket clone(){
			return new GPSLocationPacket(time, accuracy, bearing, speed, altitude, latitude, longitude);
		}

	}

}
