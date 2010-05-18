/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
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
			
			//Get the custom preferences
			Preference autoStartAtAppStart = (Preference) findPreference("autoStartAtAppStart");
			autoStartAtAppStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					return true;
				}
			});
			
			Preference autoStartAtPhoneBoot = (Preference) findPreference("autoStartAtPhoneBoot");
			autoStartAtPhoneBoot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					return true;
				}
			});
	}
}
