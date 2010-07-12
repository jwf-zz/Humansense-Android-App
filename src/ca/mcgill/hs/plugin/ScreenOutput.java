package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.plugin.BluetoothLogger.BluetoothPacket;
import ca.mcgill.hs.plugin.GPSLogger.GPSLoggerPacket;
import ca.mcgill.hs.plugin.GSMLogger.GSMLoggerPacket;
import ca.mcgill.hs.plugin.WifiLogger.WifiLoggerPacket;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to
 * the Android's logcat.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class ScreenOutput extends OutputPlugin {

	// Boolean ON-OFF switch *Temporary only*
	private boolean PLUGIN_ACTIVE;

	// The Context for the preferences.
	private final Context context;

	/**
	 * This is the basic constructor for the ScreenOutput plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a Context.
	 * 
	 * @param context
	 *            the context in which this plugin is created.
	 */
	public ScreenOutput(final Context context) {
		this.context = context;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		PLUGIN_ACTIVE = prefs.getBoolean("screenOutputEnable", false);
	}

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				"screenOutputEnable", "ScreenOutput Plugin",
				"Enables or disables this plugin.", "ScreenOutput is on.",
				"ScreenOutput is off.");

		return prefs;
	}

	/**
	 * Returns whether or not this OutputPlugin has Preferences.
	 * 
	 * @return whether or not this OutputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	/**
	 * Parses and writes given BluetoothPacket to the Android's logcat.
	 * 
	 * @param gpslp
	 *            the BluetoothPacket to parse and write out.
	 */
	private void dataParse(final BluetoothPacket bp) {
		Log.i("BluetoothLogger SO", "Bluetooth Device Found");
		Log.i("BluetoothLogger SO", "Name : " + bp.names.toString());
		Log.i("BluetoothLogger SO", "Address : " + bp.addresses.toString());
	}

	/**
	 * Parses and writes given GPSLoggerPacket to the Android's logcat.
	 * 
	 * @param gpslp
	 *            the GPSLoggerPacket to parse and write out.
	 */
	private void dataParse(final GPSLoggerPacket gpslp) {
		Log.i("GPSLocationLogger SO", "Area: [" + gpslp.altitude + "]["
				+ gpslp.latitude + "][" + gpslp.longitude + "]");
	}

	/**
	 * Parses and writes given GSMLoggerPacket to the Android's logcat.
	 * 
	 * @param gsmlp
	 *            the GSMLoggerPacket to parse and write out.
	 */
	private void dataParse(final GSMLoggerPacket gsmlp) {
		Log.i("GSMLogger SO", "Timestamp : " + gsmlp.time);
		Log.i("GSMLogger SO", "MCC : " + gsmlp.mcc);
		Log.i("GSMLogger SO", "MNC : " + gsmlp.mnc);
		Log.i("GSMLogger SO", "CID : " + gsmlp.cid);
		Log.i("GSMLogger SO", "LAC : " + gsmlp.lac);
		Log.i("GSMLogger SO", "RSSI : " + gsmlp.rssi);
		Log.i("GSMLogger SO", "Neighbors : " + gsmlp.neighbors);
		for (int i = gsmlp.neighbors - 1; i >= 0; i--) {
			Log.i("GSMLogger SO", "Neighbor " + i + " CID : " + gsmlp.cids[i]);
			Log.i("GSMLogger SO", "Neighbor " + i + " LAC : " + gsmlp.lacs[i]);
			Log
					.i("GSMLogger SO", "Neighbor " + i + " RSSI : "
							+ gsmlp.rssis[i]);
		}
	}

	/**
	 * Parses and writes given WifiLoggerPacket to the Android's logcat.
	 * 
	 * @param wlp
	 *            the WifiLoggerPacket to parse and write out.
	 */
	private void dataParse(final WifiLoggerPacket wlp) {
		Log.i("WifiLogger SO", "Time: " + wlp.timestamp);
		Log.i("WifiLogger SO", "Neighbors: " + wlp.neighbors);
		final int j = wlp.levels.length;
		for (int i = 0; i < j; i++) {
			Log.i("WifiLogger SO", "SSID: " + wlp.SSIDs[i]);
			Log.i("WifiLogger SO", "Level: " + wlp.levels[i]);
			Log.i("WifiLogger SO", "BSSID: " + wlp.BSSIDs[i]);
			Log.i("WifiLogger SO", " ");
		}
	}

	/**
	 * This method gets called whenever an InputPlugin registered to
	 * ScreenOutput has data available to output. This method calls the
	 * appropriate version of dataParse based on the DataPacket type.
	 * 
	 * @param dp
	 *            the DataPacket recieved.
	 */
	@Override
	void onDataReceived(final DataPacket dp) {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		if (dp.getClass() == WifiLoggerPacket.class) {
			dataParse((WifiLoggerPacket) dp);
		} else if (dp.getClass() == GPSLoggerPacket.class) {
			dataParse((GPSLoggerPacket) dp);
		} else if (dp.getClass() == GSMLoggerPacket.class) {
			dataParse((GSMLoggerPacket) dp);
		} else if (dp.getClass() == BluetoothPacket.class) {
			dataParse((BluetoothPacket) dp);
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean(
				"screenOutputEnable", false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}
	}

}
