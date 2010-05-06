package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class HSAndroidPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			//Get the custom preference
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
