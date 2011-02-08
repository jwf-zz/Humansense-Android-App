/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.plugin.InputPlugin;
import ca.mcgill.hs.serv.HSService;
import ca.mcgill.hs.util.Log;

/**
 * InputPluginPreferenes is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid InputPlugin objects. Whenever the user
 * accesses the "Input Plugins" option from the Settings menu, this
 * PreferenceActivity is launched. All the real work is done by the
 * {@link PreferenceFactory#createPreferenceHierarchy(PreferenceActivity, Class[])}
 * method.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class InputPluginPreferences extends PreferenceActivity {
	private static final String TAG = "InputPluginPreferences";

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
