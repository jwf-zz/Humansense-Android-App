package ca.mcgill.hs.prefs;

import java.lang.reflect.InvocationTargetException;

import ca.mcgill.hs.serv.HSService;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OutputPluginPreferences extends PreferenceActivity{
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try{
			setPreferenceScreen(createPreferenceHierarchy());
		} catch (IllegalArgumentException e) { e.printStackTrace(); }
		catch (SecurityException e) {e.printStackTrace();}
		catch (IllegalAccessException e) {e.printStackTrace();}
		catch (InvocationTargetException e) {e.printStackTrace();}
		catch (NoSuchMethodException e) {e.printStackTrace();}
	}
	
	private PreferenceScreen createPreferenceHierarchy() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException{
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		for (Class c : HSService.outputPluginsAvailable){
			if ((Boolean) c.getMethod("hasPreferences", null).invoke(null, null)){
				PreferenceCategory newCategory = new PreferenceCategory(this);
				newCategory.setTitle(c.getSimpleName() + " Preferences");
				root.addPreference(newCategory);
				for (Preference p : (Preference[]) c.getMethod("getPreferences", Context.class).invoke(null, this)){
					if (p != null) newCategory.addPreference(p);
				}
			}
		}
		
		return root;
	}

}
