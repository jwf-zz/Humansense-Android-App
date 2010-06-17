package ca.mcgill.hs.plugin;

import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.plugin.WifiLogger.WifiLoggerPacket;
import ca.mcgill.hs.plugin.GPSLogger.GPSLocationPacket;
import android.util.Log;


/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to the
 * Android's logcat.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class ScreenOutput extends OutputPlugin{

	/**
	 * This method gets called whenever an InputPlugin registered to ScreenOutput has data available
	 * to output. This method calls the appropriate version of dataParse based on the DataPacket type.
	 * 
	 * @param dp the DataPacket recieved.
	 * 
	 * @override
	 */
	void onDataReceived(DataPacket dp) {
		if (dp.getClass() == WifiLoggerPacket.class){
			dataParse((WifiLoggerPacket) dp);
		} else if (dp.getClass() == GPSLocationPacket.class) {
			dataParse((GPSLocationPacket) dp);
		} else if (dp.getClass() == SensorLoggerPacket.class){
			
		}
	}
	
	/**
	 * Parses and writes given WifiLoggerPacket to the Android's logcat.
	 * @param wlp the WifiLoggerPacket to parse and write out.
	 */
	private void dataParse(WifiLoggerPacket wlp){
		Log.i("ScreenOutput", "Time: " + wlp.timestamp);
		Log.i("ScreenOutput", "Neighbors: " + wlp.neighbors);
		int j = wlp.levels.length;
		for (int i = 0; i<j ; i++){
			Log.i("WifiLogger SO", "SSID: " + wlp.SSIDs[i]);
			Log.i("ScreenOutput", "Level: " + wlp.levels[i]);
			Log.i("ScreenOutput", "BSSID: " + wlp.BSSIDs[i]);
			Log.i("ScreenOutput", " ");
		}
	}
	
	/**
	 * Parses and writes given GPSLoggerPacket to the Android's logcat.
	 * @param gpslp the GPSLoggerPacket to parse and write out.
	 */
	private void dataParse(GPSLocationPacket gpslp){
		Log.i("GPSLocationLogger SO", "Area: [" + gpslp.altitude + "][" + gpslp.latitude + "][" + gpslp.longitude + "]");
	}

}
