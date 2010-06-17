package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.util.List;

import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Reads Wifi data.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLogger extends InputPlugin{
	
	private Thread wifiLoggerThread;
	private boolean threadRunning = false;
	private final WifiManager wm;
	private static int sleepIntervalMillisecs;
	private WifiLoggerReceiver wlr;
	private final Context context;
	
	int numResults;
	long timestamp;
	int[] levels;
	String[] SSIDs;
	String[] BSSIDs;
		
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
		
		SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(context);
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString("wifiIntervalPreference", "30000"));
	}
	
	/**
	 * This method starts the WifiLogger plugin and launches all appropriate threads. It
	 * also registers a new WifiLoggerReceiver to scan for possible network connections.
	 * This method must be overridden in all input plugins.
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
					Log.e("WifiLogger", "Logging thread terminated due to InterruptedException.");
				}
			}
		};
		wifiLoggerThread.start();
		threadRunning = true;
	}

	/**
	 * This method stops the thread if it is running, and does nothing if it is not.
	 * This method must be overridden in all input plugins.
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
		
		numResults = results.size();
		timestamp = System.currentTimeMillis();
		levels = new int[numResults];
		SSIDs = new String[numResults];
		BSSIDs = new String[numResults];
		
		int i = 0;
		for (ScanResult sr : results){
			levels[i] = sr.level;
			SSIDs[i] = sr.SSID;
			BSSIDs[i] = sr.BSSID;
			i++;
		}
		
		write(new WifiLoggerPacket(numResults, timestamp, levels, SSIDs, BSSIDs));
		
	}
	
	public static Preference[] getPreferences(Context c) {
		Preference[] prefs = new Preference[1];
		
		prefs[0] = PreferenceFactory.getListPreference(c, R.array.wifiLoggerIntervalStrings,
				R.array.wifiLoggerIntervalValues, "30000", "wifiIntervalPreference",
				R.string.wifilogger_interval_pref, R.string.wifilogger_interval_pref_summary);
		
		return prefs;
	}
	
	public static boolean hasPreferences(){ return true; }
	
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
			Log.i("WifiLogger", "Received wifi results.");
			processResults(results);
		}
		
		private final WifiManager wifi;
	}
	
	// ***********************************************************************************
	// PUBLIC INNER CLASS -- WifiLoggerPacket
	// ***********************************************************************************
	
	public class WifiLoggerPacket implements DataPacket{
		
		public final int neighbors;
		public final long timestamp;
		public final int[] levels;
		public final String[] SSIDs;
		public final String[] BSSIDs;
		
		/**
		 * Constructor for this DataPacket.
		 * @param neighbors the number of access points detected.
		 * @param timestamp the time of the scan.
		 * @param level	the signal strength level of each access point.
		 * @param SSID the SSID of each access point.
		 * @param BSSID the BSSID of each access point.
		 */
		public WifiLoggerPacket(int neighbors, long timestamp, int[] level, String[] SSID, String[] BSSID){
			this.neighbors = neighbors;
			this.timestamp = timestamp;
			this.levels = level;
			this.SSIDs = SSID;
			this.BSSIDs = BSSID;
		}

		@Override
		public String getInputPluginName() {
			return "WifiLogger";
		}
		
		@Override
		public DataPacket clone(){
			return new WifiLoggerPacket(neighbors, timestamp, levels, SSIDs, BSSIDs);
		}
	}
}
