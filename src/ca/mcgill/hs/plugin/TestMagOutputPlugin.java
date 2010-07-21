package ca.mcgill.hs.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.graph.NotificationLauncher;
import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.util.PreferenceFactory;

public class TestMagOutputPlugin extends OutputPlugin {

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

	// Boolean ON-OFF switch *Temporary only*
	private final boolean PLUGIN_ACTIVE;

	private final float[] magValues;
	private int index;
	private final int maxIndex;
	private final Context c;

	private long startTimestamp;

	private long endTimestamp;

	public TestMagOutputPlugin(final Context c, final int maxindex) {
		this.c = c;

		maxIndex = maxindex;
		magValues = new float[maxIndex];

		index = 0;

		Log.i("TEST", "Max Index: " + maxIndex);
		Log.i("TEST", "Tester is AWN.");

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);

		PLUGIN_ACTIVE = prefs.getBoolean("testMagOutputEnable", false);
	}

	private void arrayFull() {
		Log.i("TEST", "Logger is FOOL.");

		final Intent i = new Intent(c, NotificationLauncher.class);

		NotificationLauncher.setStartTimestamp(startTimestamp);
		NotificationLauncher.setEndTimestamp(endTimestamp);
		NotificationLauncher.setMagValues(magValues);

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
			index++;
		}
	}
}
