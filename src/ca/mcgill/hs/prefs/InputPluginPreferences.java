package ca.mcgill.hs.prefs;

import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.WifiLogger;
import ca.mcgill.hs.serv.HSService;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

public class InputPluginPreferences extends PreferenceActivity{
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setPreferenceScreen(createPreferenceHierarchy());
	}
	
	private PreferenceScreen createPreferenceHierarchy(){
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		PreferenceCategory wifiLoggerCategory = new PreferenceCategory(this);
		wifiLoggerCategory.setTitle(R.string.wifilogger_preferences);
		root.addPreference(wifiLoggerCategory);
		for (Preference p : WifiLogger.getPreferences(this)){
			wifiLoggerCategory.addPreference(p);
		}
		
		return root;
	}

}
