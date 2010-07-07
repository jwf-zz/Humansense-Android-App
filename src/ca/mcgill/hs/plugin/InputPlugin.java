package ca.mcgill.hs.plugin;

import ca.mcgill.hs.serv.HSService;
import android.content.Context;
import android.preference.Preference;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for using InputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
		
	/**
	 * Passes the given DataPacket to HSService.onDataReady().
	 * @param dp the given DataPacket to pass.
	 */
	protected void write(DataPacket dp){
		HSService.onDataReady(dp, this);
	}
	
	/**
	 * This method returns an array of Preference objects for the given InputPlugin. By default,
	 * this method returns null. If a specific InputPlugin wants to define Preferences, they must
	 * override this method.
	 * 
	 * @param c the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(Context c){return null;}
	
	/**
	 * Returns whether or not this InputPlugin has Preferences. By default, this method returns
	 * false. If a given InputPlugin overrides the getPreferences(Context) method, they must also
	 * override this method to let it return true.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences() {return false;}
	
	/**
	 * Signals the plugin that preferences have changed. InputPlugin objects with preferences
	 * should override this method if something changes at runtime when preferences change. If this
	 * method is not overridden in a new plugin implementation, the preferences for that plugin
	 * will only update the next time the service is started.
	 */
	public void onPreferenceChanged() {}

}
