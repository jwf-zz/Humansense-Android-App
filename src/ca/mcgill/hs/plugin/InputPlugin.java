package ca.mcgill.hs.plugin;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.serv.HSService;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for
 * using InputPlugins.
 */
public abstract class InputPlugin implements Plugin {

	/**
	 * This method returns an array of Preference objects for the given
	 * InputPlugin. By default, this method returns null. If a specific
	 * InputPlugin wants to define Preferences, they must override this method.
	 * 
	 * @param activity
	 *            The PreferenceActivity in which the preferences will appear
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		return null;
	}

	/**
	 * Returns whether or not this InputPlugin has Preferences. By default, this
	 * method returns false. If a given InputPlugin overrides the
	 * getPreferences(Context) method, they must also override this method to
	 * let it return true.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return false;
	}

	/**
	 * Called when this InputPlugin is started. This method is meant to be
	 * overridden.
	 */
	protected abstract void onPluginStart();

	/**
	 * Called when this InputPlugin is stopped. This method is meant to be
	 * overridden.
	 */
	protected abstract void onPluginStop();

	/**
	 * Signals the plugin that preferences have changed. InputPlugin objects
	 * with preferences should override this method if something changes at
	 * runtime when preferences change. If this method is not overridden in a
	 * new plugin implementation, the preferences for that plugin will only
	 * update the next time the service is started.
	 */
	public void onPreferenceChanged() {
	}

	/**
	 * Starts the plugin and calls onPluginStart().
	 */
	public final void startPlugin() {
		onPluginStart();
	}

	/**
	 * Stops the plugin and calls onPluginStop().
	 */
	public final void stopPlugin() {
		onPluginStop();
	}

	/**
	 * Passes the given DataPacket to HSService.onDataReady().
	 * 
	 * @param packet
	 *            the given DataPacket to pass.
	 */
	protected void write(final DataPacket packet) {
		HSService.onDataReady(packet, this);
	}

}
