package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import ca.mcgill.hs.serv.*;

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
					SharedPreferences prefs = 
			    		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			    	boolean autoStartAtPhoneBoot = prefs.getBoolean("autoStartAtPhoneBoot", false);
			    	if (!autoStartAtPhoneBoot){
			    		
			    	} else {
			    		
			    	}
					return true;
				}

			});
	}
}
