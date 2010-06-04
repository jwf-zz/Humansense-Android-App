package ca.mcgill.hs.plugin;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class GPSLocationLogger extends InputPlugin{
	private final LocationManager gpsm;
	private final GPSLocationListener gpsll;
	public int MIN_DIST = 0;
	public int UPDATE_FREQ = 0;
	
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
