package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

public class InputPluginPreferences extends PreferenceActivity{
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.inputpluginpreferences);
		
		Preference wifiLoggerPrefs = (Preference) findPreference("wifiLoggerPrefs");
		wifiLoggerPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(getBaseContext(), ca.mcgill.hs.prefs.WifiLoggerPreferences.class);
	            startActivity(i);
	            return true;
			}
		});
		
	}

}
