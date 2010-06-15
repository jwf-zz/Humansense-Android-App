package ca.mcgill.hs.util;

import android.content.Context;
import android.preference.ListPreference;

/**
 * An API allowing pluging programmers to easily generate preference objects.
 *  
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class PreferenceFactory {
	
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
		
		return result;
	}

}
