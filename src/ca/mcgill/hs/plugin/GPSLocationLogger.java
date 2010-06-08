package ca.mcgill.hs.plugin;

import java.util.prefs.Preferences;

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

public class GPSLocationLogger extends InputPlugin{
	private final LocationManager gpsm;
	private final GPSLocationListener gpsll;
	private int MIN_DIST = 0;
	private int UPDATE_FREQ = 0;
	
	public GPSLocationLogger(LocationManager gpsm){
		this.gpsm = gpsm;
		gpsll = new GPSLocationListener(gpsm);
	}

	/**
	 * @override
	 */
	public void startPlugin() {
		gpsm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				UPDATE_FREQ,
				MIN_DIST, 
				gpsll, Looper.getMainLooper());
		Log.i("GPSLocationLogger local", "Registered Location Listener.");
	}

	/**
	 * @override
	 */
	public void stopPlugin() {
		gpsm.removeUpdates(gpsll);
		Log.i("GPSLocationLogger local", "Unregistered Location Listener.");
	}
	
	private void getNewLocation(Location loc){
		Log.i("GPSLocationLogger local", "Data recieved.");
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

}
