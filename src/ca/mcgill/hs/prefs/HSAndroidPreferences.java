/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import ca.mcgill.hs.R;

/**
 * HSAndroidPreferences is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid Activity. Whenever the user accesses the
 * settings from the options menu, this PreferenceActivity is launched.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class HSAndroidPreferences extends PreferenceActivity {

	/**
	 * This is called when the PreferenceActivity is requested and created. This
	 * allows the user to visually see the preferences menu on the screen.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final Preference inputPrefs = findPreference("inputPluginPrefs");
		inputPrefs
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						final Intent i = new Intent(getBaseContext(),
								ca.mcgill.hs.prefs.InputPluginPreferences.class);
						startActivity(i);
						return true;
					}
				});

		final Preference outputPrefs = findPreference("outputPluginPrefs");
		outputPrefs
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(final Preference preference) {
						final Intent i = new Intent(
								getBaseContext(),
								ca.mcgill.hs.prefs.OutputPluginPreferences.class);
						startActivity(i);
						return true;
					}
				});
	}
}
