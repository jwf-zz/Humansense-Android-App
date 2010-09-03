package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.BluetoothLogger.BluetoothPacket;
import ca.mcgill.hs.plugin.GPSLogger.GPSPacket;
import ca.mcgill.hs.plugin.GSMLogger.GSMPacket;
import ca.mcgill.hs.plugin.WifiLogger.WifiPacket;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to
 * the Android's logcat.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public final class ScreenOutput extends OutputPlugin {

	private static final String SCREEN_OUTPUT_ENABLE_PREF = "screenOutputEnable";

	private static final String PLUGIN_NAME = "ScreenOutput";

	// Keeps track of whether this plugin is enabled or not.
	private static boolean pluginEnabled;

	private static SharedPreferences prefs;

	/**
	 * Parses and writes given BluetoothPacket to the Android's logcat.
	 * 
	 * @param gpslp
	 *            the BluetoothPacket to parse and write out.
	 */
	private static void dataParse(final BluetoothPacket bp) {
		Log.i(PLUGIN_NAME, "Bluetooth Device Found");
		Log.i(PLUGIN_NAME, "Name : " + bp.names.toString());
		Log.i(PLUGIN_NAME, "Address : " + bp.addresses.toString());
	}

	/**
	 * Parses and writes given GPSLoggerPacket to the Android's logcat.
	 * 
	 * @param gpslp
	 *            the GPSLoggerPacket to parse and write out.
	 */
	private static void dataParse(final GPSPacket gpslp) {
		Log.i(PLUGIN_NAME, "Area: [" + gpslp.altitude + "][" + gpslp.latitude
				+ "][" + gpslp.longitude + "]");
	}

	/**
	 * Parses and writes given GSMLoggerPacket to the Android's logcat.
	 * 
	 * @param gsmlp
	 *            the GSMLoggerPacket to parse and write out.
	 */
	private static void dataParse(final GSMPacket gsmlp) {
		Log.i(PLUGIN_NAME, "Timestamp : " + gsmlp.time);
		Log.i(PLUGIN_NAME, "MCC : " + gsmlp.mcc);
		Log.i(PLUGIN_NAME, "MNC : " + gsmlp.mnc);
		Log.i(PLUGIN_NAME, "CID : " + gsmlp.cid);
		Log.i(PLUGIN_NAME, "LAC : " + gsmlp.lac);
		Log.i(PLUGIN_NAME, "RSSI : " + gsmlp.rssi);
		Log.i(PLUGIN_NAME, "Neighbors : " + gsmlp.neighbors);
		for (int i = gsmlp.neighbors - 1; i >= 0; i--) {
			Log.i(PLUGIN_NAME, "Neighbor " + i + " CID : " + gsmlp.cids[i]);
			Log.i(PLUGIN_NAME, "Neighbor " + i + " LAC : " + gsmlp.lacs[i]);
			Log.i(PLUGIN_NAME, "Neighbor " + i + " RSSI : " + gsmlp.rssis[i]);
		}
	}

	/**
	 * Parses and writes given WifiLoggerPacket to the Android's logcat.
	 * 
	 * @param wlp
	 *            the WifiLoggerPacket to parse and write out.
	 */
	private static void dataParse(final WifiPacket wlp) {
		Log.i(PLUGIN_NAME, "Time: " + wlp.timestamp);
		Log.i(PLUGIN_NAME, "Neighbors: " + wlp.neighbors);
		final int j = wlp.levels.length;
		for (int i = 0; i < j; i++) {
			Log.i(PLUGIN_NAME, "SSID: " + wlp.SSIDs[i]);
			Log.i(PLUGIN_NAME, "Level: " + wlp.levels[i]);
			Log.i(PLUGIN_NAME, "BSSID: " + wlp.BSSIDs[i]);
			Log.i(PLUGIN_NAME, " ");
		}
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
				SCREEN_OUTPUT_ENABLE_PREF,
				R.string.screenoutput_enable_pref_label,
				R.string.screenoutput_enable_pref_summary,
				R.string.screenoutput_enable_pref_on,
				R.string.screenoutput_enable_pref_off);

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
	 * This is the basic constructor for the ScreenOutput plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a Context.
	 * 
	 * @param context
	 *            the context in which this plugin is created.
	 */
	public ScreenOutput(final Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		pluginEnabled = prefs.getBoolean(SCREEN_OUTPUT_ENABLE_PREF, false);
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
		if (!pluginEnabled) {
			return;
		}
		final int id = dp.getDataPacketId();
		if (id == WifiPacket.PACKET_ID) {
			dataParse((WifiPacket) dp);
		} else if (id == GPSPacket.PACKET_ID) {
			dataParse((GPSPacket) dp);
		} else if (id == GSMPacket.PACKET_ID) {
			dataParse((GSMPacket) dp);
		} else if (id == BluetoothPacket.PACKET_ID) {
			dataParse((BluetoothPacket) dp);
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				SCREEN_OUTPUT_ENABLE_PREF, false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		}
	}

}
