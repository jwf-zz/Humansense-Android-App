/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.util.Log;
import ca.mcgill.hs.plugin.InputPlugin;
import ca.mcgill.hs.serv.HSService;

/**
 * InputPluginPreferenes is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid InputPlugin objects. Whenever the user
 * accesses the "Input Plugins" option from the Settings menu, this
 * PreferenceActivity is launched.
 */
public class InputPluginPreferences extends PreferenceActivity {
	private static final String TAG = "InputPluginPreferences";

	/**
	 * This is called when the PreferenceActivity is requested and created. This
	 * allows the user to visually see the preferences menu on the screen. This
	 * method calls the private method createPreferenceHierarchy() in order to
	 * generate the Preference menu from the available InputPlugin objects.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Class<? extends InputPlugin>[] plugins = HSService
				.getInputPluginClasses();
		Log.d(TAG, "Generating preferences for " + plugins.length
				+ " input plugins.");
		setPreferenceScreen(PreferenceFactory.createPreferenceHierarchy(this,
				plugins));
	}
}
