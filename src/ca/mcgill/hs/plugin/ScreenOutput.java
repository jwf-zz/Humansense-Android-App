package ca.mcgill.hs.plugin;

import ca.mcgill.hs.plugin.WifiLogger.WifiLoggerPacket;
import ca.mcgill.hs.plugin.BluetoothLogger.BluetoothPacket;
import ca.mcgill.hs.plugin.GPSLogger.GPSLoggerPacket;
import ca.mcgill.hs.plugin.GSMLogger.GSMLoggerPacket;
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
		} else if (dp.getClass() == GPSLoggerPacket.class) {
			dataParse((GPSLoggerPacket) dp);
		} else if (dp.getClass() == GSMLoggerPacket.class){
			dataParse((GSMLoggerPacket) dp);
		} else if (dp.getClass() == BluetoothPacket.class){
			dataParse((BluetoothPacket) dp);
		}
	}
	
	/**
	 * Parses and writes given WifiLoggerPacket to the Android's logcat.
	 * @param wlp the WifiLoggerPacket to parse and write out.
	 */
	private void dataParse(WifiLoggerPacket wlp){
		Log.i("WifiLogger SO", "Time: " + wlp.timestamp);
		Log.i("WifiLogger SO", "Neighbors: " + wlp.neighbors);
		int j = wlp.levels.length;
		for (int i = 0; i<j ; i++){
			Log.i("WifiLogger SO", "SSID: " + wlp.SSIDs[i]);
			Log.i("WifiLogger SO", "Level: " + wlp.levels[i]);
			Log.i("WifiLogger SO", "BSSID: " + wlp.BSSIDs[i]);
			Log.i("WifiLogger SO", " ");
		}
	}
	
	/**
	 * Parses and writes given GPSLoggerPacket to the Android's logcat.
	 * @param gpslp the GPSLoggerPacket to parse and write out.
	 */
	private void dataParse(GPSLoggerPacket gpslp){
		Log.i("GPSLocationLogger SO", "Area: [" + gpslp.altitude + "][" + gpslp.latitude + "][" + gpslp.longitude + "]");
	}
	
	private void dataParse(GSMLoggerPacket gsmlp){
		Log.i("GSMLogger SO", "Timestamp : " + gsmlp.time);
		Log.i("GSMLogger SO", "MCC : " + gsmlp.mcc);
		Log.i("GSMLogger SO", "MNC : " + gsmlp.mnc);
		Log.i("GSMLogger SO", "CID : " + gsmlp.cid);
		Log.i("GSMLogger SO", "LAC : " + gsmlp.lac);
		Log.i("GSMLogger SO", "RSSI : " + gsmlp.rssi);
		Log.i("GSMLogger SO", "Neighbors : " + gsmlp.neighbors);
		for (int i = gsmlp.neighbors - 1; i >= 0; i--){
			Log.i("GSMLogger SO", "Neighbor " + i + " CID : " + gsmlp.cids[i]);
			Log.i("GSMLogger SO", "Neighbor " + i + " LAC : " + gsmlp.lacs[i]);
			Log.i("GSMLogger SO", "Neighbor " + i + " RSSI : " + gsmlp.rssis[i]);
		}
	}
	
	private void dataParse(BluetoothPacket bp){
		Log.i("BluetoothLogger SO", "Bluetooth Device Found");
		Log.i("BluetoothLogger SO", "Name : " + bp.names.toString());
		Log.i("BluetoothLogger SO", "Address : " + bp.addresses.toString());
	}

}
