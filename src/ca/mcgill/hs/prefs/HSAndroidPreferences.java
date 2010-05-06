package ca.mcgill.hs.prefs;

import java.io.File;
import java.io.IOException;

import ca.mcgill.hs.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

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
					File f = new File("/sdcard/autoStartAtPhoneBoot.prfs");
					SharedPreferences prefs = 
			    		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			    	if(prefs.getBoolean("autoStartAtPhoneBoot", false)){
			    		if (!f.exists()){
			    			Log.i(getClass().getSimpleName(), "Boot enabled");
			    			try {
								f.createNewFile();
								Log.i(getClass().getSimpleName(), "File created");
							} catch (IOException e) {
								e.printStackTrace();
							}
			    		}
			    	} else {
			    		Log.i(getClass().getSimpleName(), "Boot disabled");
			    		if (f.exists()){
			    			f.delete();
			    			Log.i(getClass().getSimpleName(), "File deleted");
			    		}
			    	}
					return true;
				}

			});
	}
}
