package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.graph.NewActivityNotificationLauncher;
import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.util.PreferenceFactory;

public class TestMagOutputPlugin extends OutputPlugin {

	// Boolean ON-OFF switch *Temporary only*
	private final boolean PLUGIN_ACTIVE;

	private final float[] magValues;
	private final int[] magActivities;

	private int index;

	private final int maxIndex;
	private final Context c;
	private long startTimestamp;
	private long endTimestamp;

	public TestMagOutputPlugin(final Context c, final int maxindex) {
		this.c = c;

		maxIndex = maxindex;
		magValues = new float[maxIndex];
		magActivities = new int[maxIndex];

		index = 0;

		Log.i("TEST", "Max Index: " + maxIndex);
		Log.i("TEST", "Tester is AWN.");

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);

		PLUGIN_ACTIVE = prefs.getBoolean("testMagOutputEnable", false);
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
				"testMagOutputEnable", "TestMag Plugin",
				"Enables or disables this plugin.", "TestMag is on.",
				"TestMag is off.");

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

	private void arrayFull() {
		Log.i("TEST", "Logger is FOOL.");

		final Intent i = new Intent(c, NewActivityNotificationLauncher.class);

		NewActivityNotificationLauncher.setStartTimestamp(startTimestamp);
		NewActivityNotificationLauncher.setEndTimestamp(endTimestamp);
		NewActivityNotificationLauncher.setMagValues(magValues, magActivities);

		c.startService(i);

		index = 0;
	}

	@Override
	void onDataReceived(final DataPacket dp) {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		final long timestamp = System.currentTimeMillis();
		if (dp.getClass() == SensorLoggerPacket.class) {
			if (index >= maxIndex) {
				endTimestamp = timestamp;
				arrayFull();
			}
			if (index == 0) {
				startTimestamp = timestamp;
			}
			magValues[index] = ((SensorLoggerPacket) dp).m;
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
		}/*
		 * for (int i = 30; i < 36; i++) { magActivities[i] = 0x5; } for (int i
		 * = 36; i < 42; i++) { magActivities[i] = 0x6; } for (int i = 42; i <
		 * 48; i++) { magActivities[i] = 0x7; } for (int i = 48; i < 54; i++) {
		 * magActivities[i] = 0x8; } for (int i = 54; i < 60; i++) {
		 * magActivities[i] = 0x9; } for (int i = 60; i < 66; i++) {
		 * magActivities[i] = 0xA; } for (int i = 66; i < 72; i++) {
		 * magActivities[i] = 0xB; } for (int i = 72; i < 78; i++) {
		 * magActivities[i] = 0xC; } for (int i = 78; i < 84; i++) {
		 * magActivities[i] = 0xD; } for (int i = 84; i < 90; i++) {
		 * magActivities[i] = 0xE; } for (int i = 90; i < 100; i++) {
		 * magActivities[i] = 0xF; }
		 */
	}
}
