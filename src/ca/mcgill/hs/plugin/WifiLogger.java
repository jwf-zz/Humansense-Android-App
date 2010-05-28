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
 * Logs the Wifi data coming from the Android's sensors. This plugin launches a thread, scans the area
 * for available Wifi connections, and then processes the results recieved from the WifiManager whenever
 * they are available.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLogger extends InputPlugin{

	//A reference to the running Thread for this input plugin.
	private Thread wifiLoggerThread;
	
	//Boolean of the current state of the plugin. If true, plugin is currently running.
	private boolean threadRunning = false;
	
	//A reference to the android's WifiManager
	private WifiManager wm;
	
	//The interval between wifi scans, in milliseconds
	private static long sleepIntervalMillisecs = 5000;
	
	//A reference to the WifiLoggerReceiver which will recieve the wifi data.
	private WifiLoggerReceiver wlr;
	
	//A reference to the Context in which this InputPlugin will be instantiated in.
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
			Log.i("WifiLogger", "Unregistered receiver.");
		}
	}
	
	/**
	 * Processes the results sent by the Wifi scan and writes them to the
	 * DataOutputStreams.
	 */
	private void processResults(List<ScanResult> results){
		Object[] scanResult = new Object[4];
		
		for (ScanResult result : results) {
			scanResult[0] = System.currentTimeMillis();
			
			scanResult[1] = result.level;
			scanResult[2] = result.SSID;
			scanResult[3] = result.BSSID;	
			
			write(scanResult);
		}
		
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- WifiLoggerReceiver
	// ***********************************************************************************
	
	/**
	 * Jordan Frank's WifiLoggerReceiver (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and
	 * modified for this plugin with his permission.
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
