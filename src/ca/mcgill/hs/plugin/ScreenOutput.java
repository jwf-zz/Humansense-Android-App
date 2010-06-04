package ca.mcgill.hs.plugin;

import ca.mcgill.hs.plugin.WifiLoggerPacket;
import android.util.Log;


/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to the
 * Android's logcat.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class ScreenOutput extends OutputPlugin{

	@Override
	void onDataReady(DataPacket dp, int sourceId) {
		if (dp.getClass() == WifiLoggerPacket.class){
			dataParse((WifiLoggerPacket) dp, sourceId);
		} else if (dp.getClass() == GPSLocationPacket.class) {
			dataParse((GPSLocationPacket) dp, sourceId);
		}
	}
	
	private void dataParse(WifiLoggerPacket wlp, int sourceId){
		//Log.i("ScreenOutput", "Time: "+wlp.timestamp);
		int j = wlp.levels.length;
		for (int i = 0; i<j ; i++){
			Log.i("WifiLogger SO", "SSID: " + wlp.SSIDs[i]);
			//Log.i("ScreenOutput", "Level: " + wlp.levels[i]);
			//Log.i("ScreenOutput", "BSSID: " + wlp.BSSIDs[i]);
			//Log.i("ScreenOutput", " ");
		}
	}
	
	private void dataParse(GPSLocationPacket gpslp, int sourceId){
		Log.i("GPSLocationLogger SO", "Area: [" + gpslp.altitude + "][" + gpslp.latitude + "][" + gpslp.longitude + "]");
	}

}
