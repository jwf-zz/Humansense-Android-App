package ca.mcgill.hs.plugin;

import ca.mcgill.hs.R;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

/**
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class GPSLogger extends InputPlugin{
	private final LocationManager gpsm;
	private final GPSLocationListener gpsll;
	private int MIN_DIST = 0;
	private int UPDATE_FREQ = 0;
	
	public GPSLogger(LocationManager gpsm){
		this.gpsm = gpsm;
		gpsll = new GPSLocationListener(gpsm);
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
		
		ListPreference intervals = new ListPreference(c);
		intervals.setEntries(R.array.gpsLoggerIntervalStrings);
		intervals.setEntryValues(R.array.gpsLoggerIntervalValues);
		intervals.setKey("gpsLoggerIntervalPreference");
		intervals.setTitle(R.string.gpslogger_interval_pref);
		intervals.setSummary(R.string.gpslogger_interval_pref_summary);
		intervals.setDefaultValue("30000");
		prefs[0] = intervals;
		
		ListPreference distance = new ListPreference(c);
		distance.setEntries(R.array.gpsLoggerDistanceStrings);
		distance.setEntryValues(R.array.gpsLoggerDistanceValues);
		distance.setKey("gpsLoggerDistancePreference");
		distance.setTitle(R.string.gpslogger_distance_pref);
		distance.setSummary(R.string.gpslogger_distance_pref_summary);
		distance.setDefaultValue("0");
		prefs[1] = distance;
		
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
