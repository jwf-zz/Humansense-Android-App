package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Logs the Wifi data. Mostly a test plugin for now.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLogger extends InputPlugin{

	private Thread wifiLoggerThread;
	private boolean threadRunning = false;
	private WifiManager wm;
	private static int sleepIntervalMillisecs = 5000;
	private WifiLoggerReceiver wlr;
	private Context context;
		
	/**
	 * This is the basic constructor for the WifiLogger plugin. It has to be instantiated
	 * before it is started, and needs to be passed a reference to a WifiManager, a Context
	 * and a WritableByteChannel (java.nio).
	 * 
	 * @param wm - the WifiManager for this WifiLogger.
	 * @param context - the context in which this plugin is created.
	 * @param wbc - the WritableByteChannel through which data will be written.
	 */
	public WifiLogger(WifiManager wm, Context context){
		this.wm = wm;
		this.context = context;
	}
	
	/**
	 * This method starts the WifiLogger plugin and launches all appropriate threads. It
	 * also registers a new WifiLoggerReceiver to scan for possible network connections.
	 * 
	 * @override
	 */
	public void startPlugin() {
		
		wlr = new WifiLoggerReceiver(wm);
		context.registerReceiver(wlr, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i("WifiLogger", "Registered receiver.");
		
		wifiLoggerThread = new Thread() {
			public void run() {
				try {
					while(threadRunning) {
						Log.i("WifiLogger", "Scanning results.");
						wm.startScan();
						sleep(sleepIntervalMillisecs);
					}
				}
				catch(InterruptedException e) {
					Log.d("WifiLogger", "Logging thread terminated due to InterruptedException.");
				}
			}
		};
		wifiLoggerThread.start();
		threadRunning = true;
	}

	/**
	 * This method stops the thread if it is running, and does nothing if it is not.
	 * 
	 * @override
	 */
	public void stopPlugin() {
		if (threadRunning){
			threadRunning = false;
			context.unregisterReceiver(wlr);
			Log.i("WifiLogger", "Unegistered receiver.");
		}
	}
	
	/**
	 * Processes the results sent by the Wifi scan and writes them to the
	 * DataOutputStream.
	 * 
	 * @throws IOException 
	 */
	private void processResults(List<ScanResult> results){
		long timestamp;
		int level;
		String SSID, BSSID;
		
		for (ScanResult result : results) {
			timestamp = System.currentTimeMillis();
			
			level = result.level;
			SSID = result.SSID;
			BSSID = result.BSSID;	
			
			write(timestamp, level, SSID, BSSID);
		}
		
	}
	
	/**
	 * Writes the results of the Wifi scan to every DataOutputStream
	 * @param timestamp the time of scan completion
	 * @param level	the level of a particular result
	 * @param SSID	the SSID of the result
	 * @param BSSID	the BSSID of the result
	 */
	private void write(long timestamp, int level, String SSID, String BSSID){
		for (DataOutputStream out : dosList){
			try {
				out.writeLong(timestamp);
				out.writeInt(level);
				out.writeUTF(SSID);
				out.writeUTF(BSSID);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- WifiLoggerReceiver
	// ***********************************************************************************
	
	/**
	 * Taken from Jordan Frank (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and
	 * modified for this plugin.
	 */
	private class WifiLoggerReceiver extends BroadcastReceiver {
		
		public WifiLoggerReceiver(WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		@Override
		public void onReceive(Context c, Intent intent) {
			final List<ScanResult> results = wifi.getScanResults();
			Log.i("WifiLogger", "Recieved wifi results.");
			processResults(results);
		}
		
		private final WifiManager wifi;
	}

}
