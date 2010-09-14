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
 * An InputPlugin which gets data from the available Wifi signals around.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public final class WifiLogger extends InputPlugin {

	/**
	 * Taken from Jordan Frank
	 * (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and modified for
	 * this plugin.
	 */
	private final class WifiLoggerReceiver extends BroadcastReceiver {

		private final WifiManager wifi;

		public WifiLoggerReceiver(final WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		@Override
		public void onReceive(final Context c, final Intent intent) {
			// Log.d(PLUGIN_NAME, "Received Wifi Scan Results.");
			final List<ScanResult> results = wifi.getScanResults();
			processResults(results);
		}
	}

	public final static class WifiPacket implements DataPacket {

		final int neighbors;
		final long timestamp;
		final int[] levels;
		final String[] SSIDs;
		final String[] BSSIDs;
		final static String PACKET_NAME = "WifiPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

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
		public WifiPacket(final int neighbors, final long timestamp,
				final int[] level, final String[] SSID, final String[] BSSID) {
			this.neighbors = neighbors;
			this.timestamp = timestamp;
			this.levels = level;
			this.SSIDs = SSID;
			this.BSSIDs = BSSID;
		}

		@Override
		public DataPacket clone() {
			return new WifiPacket(neighbors, timestamp, levels, SSIDs, BSSIDs);
		}

		@Override
		public int getDataPacketId() {
			return WifiPacket.PACKET_ID;
		}

		@Override
		public String getInputPluginName() {
			return WifiPacket.PACKET_NAME;
		}
	}

	private static final String WIFI_INTERVAL_DEFAULT = "30000";
	private static final String WIFI_INTERVAL_PREF = "wifiIntervalPreference";
	private static final String WIFI_LOGGER_ENABLE_PREF = "wifiLoggerEnable";

	final static String PLUGIN_NAME = "WifiLogger";
	final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	// Boolean ON-OFF switch *Temporary only*
	private static boolean pluginEnabled;

	// The Thread for requesting scans.
	private static Thread wifiLoggerThread;

	// A boolean detailing whether or not the Thread is running.
	private static boolean threadRunning = false;

	private static WifiManager.WifiLock wifiLock;
	private static PowerManager.WakeLock wakeLock;

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
				WIFI_LOGGER_ENABLE_PREF, R.string.wifilogger_enable_pref_label,
				R.string.wifilogger_enable_pref_summary,
				R.string.wifilogger_enable_pref_on,
				R.string.wifilogger_enable_pref_off);

		prefs[1] = PreferenceFactory.getListPreference(c,
				R.array.wifiLoggerIntervalStrings,
				R.array.wifiLoggerIntervalValues, WIFI_INTERVAL_DEFAULT,
				WIFI_INTERVAL_PREF, R.string.wifilogger_interval_pref,
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
	private final WifiManager wifiManager;

	// The interval of time between two subsequent scans.
	private static int sleepIntervalMillisecs;

	// The WifiLoggerReceiver from which we will get the Wifi scan results.
	private static WifiLoggerReceiver loggerReceiver;

	// The Context in which the WifiLoggerReceiver will be registered.
	private static Context context;

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
	 * @param context
	 *            - the context in which this plugin is created.
	 */
	public WifiLogger(final Context context) {
		WifiLogger.context = context;
		wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY,
				PLUGIN_NAME);
		wifiLock.setReferenceCounted(false);
		final PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, PLUGIN_NAME);
		wakeLock.setReferenceCounted(false);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString(
				WIFI_INTERVAL_PREF, WIFI_INTERVAL_DEFAULT));

		pluginEnabled = prefs.getBoolean(WIFI_LOGGER_ENABLE_PREF, false);
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString(
				WIFI_INTERVAL_PREF, WIFI_INTERVAL_DEFAULT));

		final boolean pluginEnabledNew = prefs.getBoolean(
				WIFI_LOGGER_ENABLE_PREF, false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
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

		write(new WifiPacket(numResults, timestamp, levels, SSIDs, BSSIDs));
		if (scanPending) {
			scanPending = false;
			// Log.d(PLUGIN_NAME, "Initiating Pending Wifi Scan.");
			wifiManager.startScan();
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
		if (!pluginEnabled) {
			return;
		}

		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
		// wl.acquire();

		loggerReceiver = new WifiLoggerReceiver(wifiManager);
		context.registerReceiver(loggerReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i(PLUGIN_NAME, "Registered receiver.");

		wifiLoggerThread = new Thread() {
			@Override
			public void run() {
				try {
					while (threadRunning) {
						if (!wifiManager.pingSupplicant()) {
							Log
									.d(PLUGIN_NAME,
											"Uh-Oh, Supplicant isn't responding to requests.");
						}
						if (!scanning) {
							scanning = true;
							// Log.d(PLUGIN_NAME, "Initiating Wifi Scan.");
							wifiManager.startScan();
						} else {
							scanPending = true;
						}
						sleep(sleepIntervalMillisecs);
					}
				} catch (final InterruptedException e) {
					Log
							.e(PLUGIN_NAME,
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
		if (!pluginEnabled) {
			return;
		}
		if (threadRunning) {
			threadRunning = false;
			context.unregisterReceiver(loggerReceiver);
			Log.i(PLUGIN_NAME, "Unegistered receiver.");
		}
		if (wifiLock.isHeld()) {
			wifiLock.release();
		}
		// wl.release();
	}
}
