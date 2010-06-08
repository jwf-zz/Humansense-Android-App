/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

/**
 * HSAndroidPreferences is a class extending PreferenceActivity which defines the settings
 * menu for the HSAndroid Activity. Whenever the user accesses the settings from the options
 * menu, this PreferenceActivity is launched.
 * 
 * @author Jonathan Pitre
 *
 */
public class HSAndroidPreferences extends PreferenceActivity {
	
	/**
	 * This is called when the PreferenceActivity is requested and created. This allows
	 * the user to visually see the preferences menu on the screen.
	 * 
	 * @override
	 */
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			
			Preference inputPrefs = (Preference) findPreference("inputPluginPrefs");
			inputPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					Intent i = new Intent(getBaseContext(), ca.mcgill.hs.prefs.InputPluginPreferences.class);
		            startActivity(i);
		            return true;
				}
			});
			
			Preference outputPrefs = (Preference) findPreference("outputPluginPrefs");
			outputPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					Intent i = new Intent(getBaseContext(), ca.mcgill.hs.prefs.OutputPluginPreferences.class);
		            startActivity(i);
		            return true;
				}
			});
	}
}
