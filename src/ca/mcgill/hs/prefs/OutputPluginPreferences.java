package ca.mcgill.hs.prefs;

import java.lang.reflect.InvocationTargetException;

import ca.mcgill.hs.serv.HSService;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

/**
 * OutputPluginPreferenes is a class extending PreferenceActivity which defines the settings
 * menu for the HSAndroid OutputPlugin objects. Whenever the user accesses the "Output Plugins" option
 * from the Settings menu, this PreferenceActivity is launched.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class OutputPluginPreferences extends PreferenceActivity{
	
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
		
		try{
			setPreferenceScreen(createPreferenceHierarchy());
		} catch (IllegalArgumentException e) { e.printStackTrace(); }
		catch (SecurityException e) {e.printStackTrace();}
		catch (IllegalAccessException e) {e.printStackTrace();}
		catch (InvocationTargetException e) {e.printStackTrace();}
		catch (NoSuchMethodException e) {e.printStackTrace();}
	}
	
	/**
	 * This method creates a PreferenceScreen from the available OutputPlugin classes.
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
