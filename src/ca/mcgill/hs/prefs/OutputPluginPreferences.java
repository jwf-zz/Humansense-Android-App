/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.plugin.OutputPlugin;
import ca.mcgill.hs.serv.HSService;
import ca.mcgill.hs.util.Log;

/**
 * OutputPluginPreferenes is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid OutputPlugin objects. Whenever the user
 * accesses the "Output Plugins" option from the Settings menu, this
 * PreferenceActivity is launched. All the real work is done by the
 * {@link PreferenceFactory#createPreferenceHierarchy(PreferenceActivity, Class[])}
 * method.
 */
public class OutputPluginPreferences extends PreferenceActivity {
	private static final String TAG = "OutputPluginPreferences";

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
