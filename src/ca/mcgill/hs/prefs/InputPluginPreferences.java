package ca.mcgill.hs.prefs;

import java.lang.reflect.InvocationTargetException;

import ca.mcgill.hs.R;
import ca.mcgill.hs.serv.HSService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;

/**
 * InputPluginPreferenes is a class extending PreferenceActivity which defines the settings
 * menu for the HSAndroid InputPlugin objects. Whenever the user accesses the "Input Plugins" option
 * from the Settings menu, this PreferenceActivity is launched.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class InputPluginPreferences extends PreferenceActivity{
	
	/**
	 * This is called when the PreferenceActivity is requested and created. This allows
	 * the user to visually see the preferences menu on the screen. This method calls the
	 * private method createPreferenceHierarchy() in order to generate the Preference menu
	 * from the available InputPlugin objects.
	 * 
	 * @override
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try{ setPreferenceScreen(createPreferenceHierarchy()); }
		catch (IllegalArgumentException e) { e.printStackTrace(); }
		catch (SecurityException e) {e.printStackTrace();}
		catch (IllegalAccessException e) {e.printStackTrace();}
		catch (InvocationTargetException e) {e.printStackTrace();}
		catch (NoSuchMethodException e) {e.printStackTrace();}
	}
	
	/**
	 * This method creates a PreferenceScreen from the available InputPlugin classes.
	 * 
	 * @return a PreferenceScreen with the appropriate Preference objects.
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private PreferenceScreen createPreferenceHierarchy() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException{
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		for (Class c : HSService.inputPluginsAvailable){
			if ((Boolean) c.getMethod("hasPreferences", null).invoke(null, null)){
				/*Preference nest = new Preference(this);
				nest.setTitle(c.getSimpleName() + " Preferences");
				
				final PreferenceScreen leaf = getPreferenceManager().createPreferenceScreen(this);
				for (Preference p : (Preference[]) c.getMethod("getPreferences", Context.class).invoke(null, this)){
					if (p != null) leaf.addPreference(p);
				}
				
				nest.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						setPreferenceScreen(leaf);
						return true;
					}
				});
				
				root.addPreference(nest);*/
				
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
