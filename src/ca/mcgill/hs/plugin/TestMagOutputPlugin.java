package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.graph.NewActivityNotificationLauncher;
import ca.mcgill.hs.plugin.SensorLogger.SensorPacket;
import ca.mcgill.hs.util.PreferenceFactory;

public final class TestMagOutputPlugin extends OutputPlugin {

	public static final String PLUGIN_NAME = "TestMagOutput";
	public static final int PLUGIN_ID = PLUGIN_NAME.hashCode();

	private static final String TESTMAG_OUTPUT_ENABLE_PREF = "testMagOutputEnable";

	// Keeps track of whether this plugin is enabled or not.
	private static boolean pluginEnabled;

	private static final int MAX_INDEX = 100;

	private static final float[] magValues = new float[MAX_INDEX];

	private static int[] magActivities = new int[MAX_INDEX];

	private static int index;
	private static SharedPreferences prefs;
	private static Context context;

	private static long startTimestamp;

	private static long endTimestamp;

	private static void arrayFull() {
		Log.i(PLUGIN_NAME, "Array is full.");

		final Intent i = new Intent(context,
				NewActivityNotificationLauncher.class);

		NewActivityNotificationLauncher.setStartTimestamp(startTimestamp);
		NewActivityNotificationLauncher.setEndTimestamp(endTimestamp);
		NewActivityNotificationLauncher.setMagValues(magValues, magActivities);

		context.startService(i);

		index = 0;
	}

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context context) {
		TestMagOutputPlugin.context = context;
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(context,
				TESTMAG_OUTPUT_ENABLE_PREF,
				R.string.testmagoutput_enable_pref_label,
				R.string.testmagoutput_enable_pref_summary,
				R.string.testmagoutput_enable_pref_on,
				R.string.testmagoutput_enable_pref_off);
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

	public TestMagOutputPlugin(final Context context) {
		index = 0;

		Log.i(PLUGIN_NAME, "Max Index: " + MAX_INDEX);

		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		pluginEnabled = prefs.getBoolean(TESTMAG_OUTPUT_ENABLE_PREF, false);
	}

	@Override
	void onDataReceived(final DataPacket packet) {
		if (!pluginEnabled) {
			return;
		}
		final long timestamp = System.currentTimeMillis();
		if (packet.getClass() == SensorPacket.class) {
			if (index >= MAX_INDEX) {
				endTimestamp = timestamp;
				arrayFull();
			}
			if (index == 0) {
				startTimestamp = timestamp;
			}
			magValues[index] = ((SensorPacket) packet).m;
			magActivities[index] = 0x0;
			index++;
		}
		for (int i = 6; i < 12; i++) {
			magActivities[i] = 0x1;
		}
		for (int i = 12; i < 18; i++) {
			magActivities[i] = 0x2;
		}
		for (int i = 18; i < 24; i++) {
			magActivities[i] = 0x3;
		}
		for (int i = 24; i < 30; i++) {
			magActivities[i] = 0x4;
		}
		for (int i = 30; i < 36; i++) {
			magActivities[i] = 0x5;
		}
		for (int i = 36; i < 42; i++) {
			magActivities[i] = 0x6;
		}
		for (int i = 42; i < 48; i++) {
			magActivities[i] = 0x3;
		}
		for (int i = 48; i < 54; i++) {
			magActivities[i] = 0x5;
		}
		for (int i = 54; i < 60; i++) {
			magActivities[i] = 0x0;
		}
		for (int i = 60; i < 66; i++) {
			magActivities[i] = 0xA;
		}
		for (int i = 66; i < 72; i++) {
			magActivities[i] = 0x2;
		}
		for (int i = 72; i < 78; i++) {
			magActivities[i] = 0xC;
		}
		for (int i = 78; i < 84; i++) {
			magActivities[i] = 0xD;
		}
		for (int i = 84; i < 90; i++) {
			magActivities[i] = 0xA;
		}
		for (int i = 90; i < 100; i++) {
			magActivities[i] = 0xF;
		}
	}
}
