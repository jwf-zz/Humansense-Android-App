package ca.mcgill.hs.plugin;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * An InputPlugin which gets data from the available GPS signals around.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class WifiLogger extends InputPlugin {

	public static class WifiLoggerPacket implements DataPacket {

		final int neighbors;
		final long timestamp;
		final int[] levels;
		final String[] SSIDs;
		final String[] BSSIDs;
		final static String PLUGIN_NAME = "WifiLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

		/**
		 * Constructor for this DataPacket.
		 * 
		 * @param neighbors
		 *            the number of access points detected.
		 * @param timestamp
		 *            the time of the scan.
		 * @param level
		 *            the signal strength level of each access point.
		 * @param SSID
		 *            the SSID of each access point.
		 * @param BSSID
		 *            the BSSID of each access point.
		 */
		public WifiLoggerPacket(final int neighbors, final long timestamp,
				final int[] level, final String[] SSID, final String[] BSSID) {
			this.neighbors = neighbors;
			this.timestamp = timestamp;
			this.levels = level;
			this.SSIDs = SSID;
			this.BSSIDs = BSSID;
		}

		@Override
		public DataPacket clone() {
			return new WifiLoggerPacket(neighbors, timestamp, levels, SSIDs,
					BSSIDs);
		}

		@Override
		public int getDataPacketId() {
			return WifiLoggerPacket.PLUGIN_ID;
		}

		@Override
		public String getInputPluginName() {
			return WifiLoggerPacket.PLUGIN_NAME;
		}
	}

	/**
	 * Taken from Jordan Frank
	 * (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and modified for
	 * this plugin.
	 */
	private class WifiLoggerReceiver extends BroadcastReceiver {

		private final WifiManager wifi;

		public WifiLoggerReceiver(final WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		@Override
		public void onReceive(final Context c, final Intent intent) {
			Log.i(TAG, "Received Wifi Scan Results.");
			final List<ScanResult> results = wifi.getScanResults();
			processResults(results);
		}
	}

	public static final String TAG = "WifiLogger";

	// Boolean ON-OFF switch *Temporary only*
	private static boolean PLUGIN_ACTIVE;

	// The Thread for requesting scans.
	private static Thread wifiLoggerThread;

	// A boolean detailing whether or not the Thread is running.
	private static boolean threadRunning = false;

	private static WifiManager.WifiLock wifiLock;
	private static PowerManager.WakeLock wl;

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[2];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				"wifiLoggerEnable", "Wi-Fi Plugin",
				"Enables or disables this plugin.", "WifiLogger is on.",
				"WifiLogger is off.");

		prefs[1] = PreferenceFactory.getListPreference(c,
				R.array.wifiLoggerIntervalStrings,
				R.array.wifiLoggerIntervalValues, "30000",
				"wifiIntervalPreference", R.string.wifilogger_interval_pref,
				R.string.wifilogger_interval_pref_summary);

		return prefs;
	}

	/**
	 * Returns whether or not this InputPlugin has Preferences.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	// A WifiManager used to request scans.
	private final WifiManager wm;

	// The interval of time between two subsequent scans.
	private static int sleepIntervalMillisecs;

	// The WifiLoggerReceiver from which we will get the Wifi scan results.
	private static WifiLoggerReceiver wlr;

	// The Context in which the WifiLoggerReceiver will be registered.
	private final Context context;

	// Keeps track of whether a scan is in progress
	private static boolean scanning = false;

	// Keeps track of whether a new scan has been initiated prior to a previous
	// scan being completed
	private static boolean scanPending = false;

	/**
	 * This is the basic constructor for the WifiLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a WifiManager and a Context.
	 * 
	 * @param wm
	 *            - the WifiManager for this WifiLogger.
	 * @param context
	 *            - the context in which this plugin is created.
	 */
	public WifiLogger(final WifiManager wm, final Context context) {
		this.wm = wm;
		this.context = context;
		wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
		wifiLock.setReferenceCounted(false);
		final PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
		wl.setReferenceCounted(false);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString(
				"wifiIntervalPreference", "30000"));

		PLUGIN_ACTIVE = prefs.getBoolean("wifiLoggerEnable", false);
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString(
				"wifiIntervalPreference", "30000"));

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean("wifiLoggerEnable",
				false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}
	}

	/**
	 * Processes the results sent by the Wifi scan and writes them out.
	 */
	private void processResults(final List<ScanResult> results) {

		final int numResults = results.size();
		final long timestamp = System.currentTimeMillis();
		final int[] levels = new int[numResults];
		final String[] SSIDs = new String[numResults];
		final String[] BSSIDs = new String[numResults];

		int i = 0;
		for (final ScanResult sr : results) {
			levels[i] = sr.level;
			SSIDs[i] = sr.SSID;
			BSSIDs[i] = sr.BSSID;
			i++;
		}

		write(new WifiLoggerPacket(numResults, timestamp, levels, SSIDs, BSSIDs));
		if (scanPending) {
			scanPending = false;
			Log.i(TAG, "Initiating Pending Wifi Scan.");
			wm.startScan();
		} else {
			scanning = false;
		}
	}

	/**
	 * This method starts the WifiLogger plugin and launches all appropriate
	 * threads. It also registers a new WifiLoggerReceiver to scan for possible
	 * network connections. This method must be overridden in all input plugins.
	 */
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}

		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
		// wl.acquire();

		wlr = new WifiLoggerReceiver(wm);
		context.registerReceiver(wlr, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i(TAG, "Registered receiver.");

		wifiLoggerThread = new Thread() {
			@Override
			public void run() {
				try {
					while (threadRunning) {
						if (!wm.pingSupplicant()) {
							Log
									.d(TAG,
											"Uh-Oh, Supplicant isn't responding to requests.");
						}
						if (!scanning) {
							scanning = true;
							Log.i(TAG, "Initiating Wifi Scan.");
							wm.startScan();
						} else {
							scanPending = true;
						}
						sleep(sleepIntervalMillisecs);
					}
				} catch (final InterruptedException e) {
					Log
							.e(TAG,
									"Logging thread terminated due to InterruptedException.");
				}
			}
		};
		wifiLoggerThread.start();
		threadRunning = true;
	}

	/**
	 * This method stops the thread if it is running, and does nothing if it is
	 * not.
	 */
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		if (threadRunning) {
			threadRunning = false;
			context.unregisterReceiver(wlr);
			Log.i(TAG, "Unegistered receiver.");
		}
		if (wifiLock.isHeld()) {
			wifiLock.release();
		}
		// wl.release();
	}
}
