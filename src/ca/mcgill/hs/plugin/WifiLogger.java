/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

/**
 * An InputPlugin which gets data from the wifi base stations that are currently
 * in listening range.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public final class WifiLogger extends InputPlugin {
	/**
	 * Receives the wifi scan results.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	private final class WifiLoggerReceiver extends BroadcastReceiver {

		private final WifiManager wifi;

		public WifiLoggerReceiver(final WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		@Override
		public void onReceive(final Context c, final Intent intent) {
			Log.d(PLUGIN_NAME, "onReceive");
			synchronized (this) {
				final List<ScanResult> results = wifi.getScanResults();
				if (results != null) {
					processResults(results);
				}
			}
		}

		/**
		 * Processes the results sent by the Wifi scan and writes them out.
		 * 
		 * @param results
		 *            The list of scan results.
		 */
		private void processResults(final List<ScanResult> results) {
			Log.d(PLUGIN_NAME, "processResults");
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
				Log.d(PLUGIN_NAME, "Scan pending, starting a new scan.");
				scanPending = false;
				wifiManager.startScan();
			} else {
				Log.d(PLUGIN_NAME, "No scan pending.");
				scanning = false;
			}
		}
	}

	/**
	 * The data packet containing wifi scan data.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	public final static class WifiPacket implements DataPacket {

		final int numAccessPoints;
		final long timestamp;
		final int[] signalStrengths;
		final String[] SSIDs;
		final String[] BSSIDs;
		final static String PACKET_NAME = "WifiPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

		/**
		 * Constructor for this DataPacket.
		 * 
		 * @param numAccessPoints
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
		public WifiPacket(final int numAccessPoints, final long timestamp,
				final int[] level, final String[] SSID, final String[] BSSID) {
			this.numAccessPoints = numAccessPoints;
			this.timestamp = timestamp;
			this.signalStrengths = level;
			this.SSIDs = SSID;
			this.BSSIDs = BSSID;
		}

		@Override
		public DataPacket clone() {
			return new WifiPacket(numAccessPoints, timestamp, signalStrengths,
					SSIDs, BSSIDs);
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

	public final static String PLUGIN_NAME = "WifiLogger";
	public final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	/**
	 * @see InputPlugin#getPreferences(PreferenceActivity)
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		Log.d(PLUGIN_NAME, "In getPreferences().");
		final Preference[] prefs = new Preference[2];
		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				WIFI_LOGGER_ENABLE_PREF, R.string.wifilogger_enable_pref_label,
				R.string.wifilogger_enable_pref_summary,
				R.string.wifilogger_enable_pref_on,
				R.string.wifilogger_enable_pref_off, false);

		prefs[1] = PreferenceFactory.getListPreference(activity,
				R.array.wifilogger_pref_interval_strings,
				R.array.wifilogger_pref_interval_values, WIFI_INTERVAL_DEFAULT,
				WIFI_INTERVAL_PREF, R.string.wifilogger_interval_pref,
				R.string.wifilogger_interval_pref_summary);
		return prefs;
	}

	/**
	 * @see InputPlugin#hasPreferences()
	 */
	public static boolean hasPreferences() {
		return true;
	}

	// A boolean detailing whether or not the Thread is running.
	private boolean threadRunning = false;

	private final WifiManager.WifiLock wifiLock;

	// private final PowerManager.WakeLock wakeLock;

	// A WifiManager used to request scans.
	private final WifiManager wifiManager;

	// The interval of time between two subsequent scans.
	private int sleepIntervalMillisecs;

	// The WifiLoggerReceiver from which we will get the Wifi scan results.
	private WifiLoggerReceiver loggerReceiver;

	// The Context in which the WifiLoggerReceiver will be registered.
	private final Context context;

	// Keeps track of whether a scan is in progress
	private boolean scanning = false;

	// Keeps track of whether a new scan has been initiated prior to a previous
	// scan being completed
	private boolean scanPending = false;

	final SharedPreferences prefs;

	/**
	 * Constructs a new wifi logger input plugin
	 * 
	 * @param context
	 *            The application context, required to access the wifi manager
	 *            and preferences.
	 * 
	 */
	public WifiLogger(final Context context) {
		this.context = context;
		prefs = PreferenceFactory.getSharedPreferences(context);

		wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		/*
		 * Obtain a scan-only wifi lock, and do our own reference counting.
		 */
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY,
				PLUGIN_NAME);
		wifiLock.setReferenceCounted(false);
		Log.d(PLUGIN_NAME, "plugin initialized");
	}

	@Override
	protected void onPluginStart() {
		/*
		 * This method starts the WifiLogger plugin and launches all appropriate
		 * threads. It also registers a new WifiLoggerReceiver to scan for
		 * possible network connections. This method must be overridden in all
		 * input plugins.
		 */
		Log.d(PLUGIN_NAME, "Starting Wifi Logger.");
		pluginEnabled = prefs.getBoolean(WIFI_LOGGER_ENABLE_PREF, false);
		updatePreferences();

		if (!pluginEnabled) {
			return;
		}
		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}

		loggerReceiver = new WifiLoggerReceiver(wifiManager);
		context.registerReceiver(loggerReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i(PLUGIN_NAME, "Registered receiver.");

		new Thread() {
			@Override
			public void run() {
				try {
					threadRunning = true;
					while (threadRunning) {
						/*
						 * This was for debugging purposes and may no longer be
						 * necessary.
						 */
						if (!wifiManager.pingSupplicant()) {
							Log
									.d(PLUGIN_NAME,
											"Uh-Oh, Supplicant isn't responding to requests.");
						}
						if (!scanning) {
							scanning = true;
							wifiManager.startScan();
						} else {
							scanPending = true;
						}
						sleep(sleepIntervalMillisecs);
					}
					Log.d(PLUGIN_NAME,
							"Scanning loop complete, thread terminating.");
				} catch (final InterruptedException e) {
					Log
							.e(PLUGIN_NAME,
									"Logging thread terminated due to InterruptedException.");
				}
			}
		}.start();
	}

	@Override
	protected void onPluginStop() {
		scanPending = false;
		scanning = false;
		threadRunning = false;
		try {
			context.unregisterReceiver(loggerReceiver);
			Log.i(PLUGIN_NAME, "Unegistered receiver.");
		} catch (final IllegalArgumentException e) {
			Log.e(PLUGIN_NAME, "Exception unregistering wifilogging receiver");
			Log.e(PLUGIN_NAME, e);
		}
		if (wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				WIFI_LOGGER_ENABLE_PREF, false);
		updatePreferences();
		super.changePluginEnabledStatus(pluginEnabledNew);
	}

	private void updatePreferences() {
		sleepIntervalMillisecs = Integer.parseInt(prefs.getString(
				WIFI_INTERVAL_PREF, WIFI_INTERVAL_DEFAULT));
	}
}
