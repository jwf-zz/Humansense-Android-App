package ca.mcgill.hs.prefs;

import java.lang.reflect.InvocationTargetException;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import ca.mcgill.hs.serv.HSService;

/**
 * InputPluginPreferenes is a class extending PreferenceActivity which defines
 * the settings menu for the HSAndroid InputPlugin objects. Whenever the user
 * accesses the "Input Plugins" option from the Settings menu, this
 * PreferenceActivity is launched.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class InputPluginPreferences extends PreferenceActivity {

	/**
	 * This method creates a PreferenceScreen from the available InputPlugin
	 * classes.
	 * 
	 * @return a PreferenceScreen with the appropriate Preference objects.
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private PreferenceScreen createPreferenceHierarchy()
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		final PreferenceScreen root = getPreferenceManager()
				.createPreferenceScreen(this);

		for (final Class<?> c : HSService.inputPluginsAvailable) {
			if ((Boolean) c.getMethod("hasPreferences", (Class[]) null).invoke(
					null, (Object[]) null)) {

				final PreferenceCategory newCategory = new PreferenceCategory(
						this);
				newCategory.setTitle(c.getSimpleName() + " Preferences");
				root.addPreference(newCategory);
				for (final Preference p : (Preference[]) c.getMethod(
						"getPreferences", Context.class).invoke(null, this)) {
					if (p != null) {
						newCategory.addPreference(p);
					}
				}
			}
		}

		return root;
	}

	/**
	 * This is called when the PreferenceActivity is requested and created. This
	 * allows the user to visually see the preferences menu on the screen. This
	 * method calls the private method createPreferenceHierarchy() in order to
	 * generate the Preference menu from the available InputPlugin objects.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			setPreferenceScreen(createPreferenceHierarchy());
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

}
