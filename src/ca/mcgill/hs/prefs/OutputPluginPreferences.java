package ca.mcgill.hs.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import ca.mcgill.hs.plugin.OutputPlugin;
import ca.mcgill.hs.serv.HSService;

/**
 * OutputPluginPreferenes is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid OutputPlugin objects. Whenever the user
 * accesses the "Output Plugins" option from the Settings menu, this
 * PreferenceActivity is launched.
 */
public class OutputPluginPreferences extends PreferenceActivity {
	private static final String TAG = "OutputPluginPreferences";

	/**
	 * This is called when the PreferenceActivity is requested and created. This
	 * allows the user to visually see the preferences menu on the screen. This
	 * method calls the private method createPreferenceHierarchy() in order to
	 * generate the Preference menu from the available InputPlugin objects.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Class<? extends OutputPlugin>[] plugins = HSService
				.getOutputPluginClasses();
		Log.d(TAG, "Generating preferences for " + plugins.length
				+ " input plugins.");
		setPreferenceScreen(PreferenceFactory.createPreferenceHierarchy(this,
				plugins));
	}

}
