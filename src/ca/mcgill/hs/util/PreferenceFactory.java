package ca.mcgill.hs.util;

import android.content.Context;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * An API allowing plugin programmers to easily generate preference objects.
 *  
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class PreferenceFactory {
	
	public static final String PREFERENCES_CHANGED_INTENT = "ca.mcgill.hs.HSAndroidApp.PREFERENCES_CHANGED_INTENT";
	
	/**
	 * Returns a ListPreference preference specified by the parameters. This method uses resource IDs
	 * for its parameters.
	 * 
	 * @param c The context for this preference.
	 * @param entriesResId The resource ID for the text entries of this preference.
	 * @param entryValuesResId The resource ID for the entry values of this preference.
	 * @param defaultValue The default value of this preference.
	 * @param key The key of this preference.
	 * @param titleResId The resource ID for the title of this preference.
	 * @param summaryResId The resource ID for the summary of this preference.
	 * @return A ListPreference object.
	 */
	public static ListPreference getListPreference(Context c, int entriesResId, int entryValuesResId,
			Object defaultValue, String key, int titleResId, int summaryResId){
		ListPreference result = new ListPreference(c);
		
		result.setEntries(entriesResId);
		result.setEntryValues(entryValuesResId);
		result.setDefaultValue(defaultValue);
		result.setKey(key);
		result.setTitle(titleResId);
		result.setSummary(summaryResId);
		
		final Context context = c;
		result.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				broadcastIntent(context);
				return false;
			}
		});
		
		return result;
	}
	
	/**
	 * Returns a ListPreference preference specified by the parameters. This method does not use
	 * resource IDs for its parameters.
	 * 
	 * @param c The context for this preference.
	 * @param entries The String array of entries for this preference.
	 * @param entryValues The String array of values for this preference.
	 * @param defaultValue The default value of this preference.
	 * @param key The key of this preference.
	 * @param title The String for the title of this preference.
	 * @param summary The String for the summary of this preference.
	 * @return A ListPreference object.
	 */
	public static ListPreference getListPreference(Context c, String[] entries, String[] entryValues,
			Object defaultValue, String key, String title, String summary){
		ListPreference result = new ListPreference(c);
		
		result.setEntries(entries);
		result.setEntryValues(entryValues);
		result.setDefaultValue(defaultValue);
		result.setKey(key);
		result.setTitle(title);
		result.setSummary(summary);
		
		final Context context = c;
		result.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				broadcastIntent(context);
				return false;
			}
		});
		
		return result;
	}
	
	/**
	 * Returns a CheckBoxPreference preference specified by the parameters. This method does not use
	 * resource IDs for its parameters.
	 * 
	 * @param c The context for this preference.
	 * @param key The key of this preference.
	 * @param titleResId The resource ID for the title of this preference.
	 * @param summaryDefResId The resource ID for the default summary of this preference.
	 * @param summaryOnResId The resource ID for the summary displayed when this checkbox is checked.
	 * @param summaryOffResId The resource ID for the summary displayed when this checkbox is unchecked.
	 * @return A CheckBoxPreference object.
	 */
	public static CheckBoxPreference getCheckBoxPreference(Context c, String key, int titleResId,
			int summaryDefResId, int summaryOnResId, int summaryOffResId){
		CheckBoxPreference cbp = new CheckBoxPreference(c);
		
		cbp.setKey(key);
		cbp.setTitle(titleResId);
		cbp.setSummary(summaryDefResId);
		cbp.setSummaryOn(summaryOnResId);
		cbp.setSummaryOff(summaryOffResId);
		
		final Context context = c;
		cbp.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				broadcastIntent(context);
				return false;
			}
		});
		
		return cbp;
	}
	
	/**
	 * Returns a CheckBoxPreference preference specified by the parameters. This method does not use
	 * resource IDs for its parameters.
	 * 
	 * @param c The context for this preference.
	 * @param key The key of this preference.
	 * @param title The String for the title of this preference.
	 * @param summaryDef The String for the default summary of this preference.
	 * @param summaryOn The String for the summary displayed when this checkbox is checked.
	 * @param summaryOff The String for the summary displayed when this checkbox is unchecked.
	 * @return A CheckBoxPreference object.
	 */
	public static CheckBoxPreference getCheckBoxPreference(Context c, String key, String title,
			String summaryDef, String summaryOn, String summaryOff){
		CheckBoxPreference cbp = new CheckBoxPreference(c);
		
		cbp.setKey(key);
		cbp.setTitle(title);
		cbp.setSummary(summaryDef);
		cbp.setSummaryOn(summaryOn);
		cbp.setSummaryOff(summaryOff);
		
		final Context context = c;
		cbp.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				broadcastIntent(context);
				return false;
			}
		});
		
		return cbp;
	}
	
	/**
	 * Broadcasts an intent whenever a preference has changed.
	 * 
	 * @param c The context in which the intent will be broadcast.
	 */
	private static void broadcastIntent(Context c){
		Intent i = new Intent();
		i.setAction(PREFERENCES_CHANGED_INTENT);
		c.sendBroadcast(i);
	}

}
