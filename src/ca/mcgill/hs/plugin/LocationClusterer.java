package ca.mcgill.hs.plugin;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.location.GPSClusterer;
import ca.mcgill.hs.classifiers.location.WifiClusterer;
import ca.mcgill.hs.classifiers.location.WifiObservation;
import ca.mcgill.hs.plugin.WifiLogger.WifiPacket;
import ca.mcgill.hs.util.PreferenceFactory;

public class LocationClusterer extends OutputPlugin {
	private static final String LOCATION_CLUSTERER_ENABLED_PREF = "locationClustererEnabledPref";

	// The preference manager for this plugin.
	private static SharedPreferences prefs;

	// Keeps track of whether this plugin is enabled or not.
	private static boolean pluginEnabled;

	private static Context context;

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
				LOCATION_CLUSTERER_ENABLED_PREF,
				R.string.locationclusterer_enable_pref_label,
				R.string.locationclusterer_enable_pref_summary,
				R.string.locationclusterer_enable_pref_on,
				R.string.locationclusterer_enable_pref_off);
		/*
		 * prefs[1] = PreferenceFactory.getListPreference(c,
		 * R.array.fileOutputPluginBufferSizeStrings,
		 * R.array.fileOutputPluginBufferSizeValues, c.getResources()
		 * .getText(R.string.fileoutput_buffersizedefault_pref),
		 * BUFFER_SIZE_KEY, R.string.fileoutput_buffersize_pref,
		 * R.string.fileoutput_buffersize_pref_summary); prefs[2] =
		 * PreferenceFactory.getListPreference(c,
		 * R.array.fileOutputPluginRolloverIntervalStrings,
		 * R.array.fileOutputPluginRolloverIntervalValues,
		 * c.getResources().getText(
		 * R.string.fileoutput_rolloverintervaldefault_pref),
		 * ROLLOVER_INTERVAL_KEY, R.string.fileoutput_rolloverinterval_pref,
		 * R.string.fileoutput_rolloverinterval_pref_summary);
		 */
		return prefs;
	}

	public static boolean hasPreferences() {
		return true;
	}

	private WifiClusterer wifiClusterer = null;

	private GPSClusterer gpsClusterer = null;

	public LocationClusterer(final Context context) {
		LocationClusterer.context = context;
		// wifiClusterer.setContext(context);

		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		pluginEnabled = prefs
				.getBoolean(LOCATION_CLUSTERER_ENABLED_PREF, false);
		/*
		 * bufferSize = Integer.parseInt(prefs.getString(BUFFER_SIZE_KEY,
		 * (String) context.getResources().getText(
		 * R.string.fileoutput_buffersizedefault_pref)));
		 * 
		 * rolloverInterval = Integer.parseInt(prefs.getString(
		 * ROLLOVER_INTERVAL_KEY, (String) context.getResources().getText(
		 * R.string.fileoutput_rolloverintervaldefault_pref)));
		 */
	}

	@Override
	void onDataReceived(final DataPacket dp) {
		if (!pluginEnabled) {
			return;
		}
		if (dp.getDataPacketId() == WifiPacket.PACKET_ID) {
			final WifiPacket packet = (WifiPacket) dp;
			final WifiObservation observation = new WifiObservation(
					packet.timestamp, packet.neighbors);
			for (int i = 0; i < packet.neighbors; i++) {
				observation.addObservation(packet.BSSIDs[i].hashCode(),
						packet.levels[i]);
			}
			wifiClusterer.cluster(packet.timestamp, observation);
		}
	}

	@Override
	protected void onPluginStart() {
		wifiClusterer = new WifiClusterer(new File(context
				.getExternalFilesDir(null), "wificlusters.db"));
		gpsClusterer = new GPSClusterer(new File(context
				.getExternalFilesDir(null), "gpsclusters.db"));
	}

	@Override
	protected void onPluginStop() {
		if (wifiClusterer != null) {
			wifiClusterer.close();
			wifiClusterer = null;
		}
		if (gpsClusterer != null) {
			gpsClusterer.close();
			wifiClusterer = null;
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				LOCATION_CLUSTERER_ENABLED_PREF, false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		}
	}

}
