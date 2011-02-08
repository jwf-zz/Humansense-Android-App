/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.serv.HSService;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for
 * using InputPlugins.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public abstract class InputPlugin implements Plugin {

	/**
	 * Returns an array of Preference objects for the given InputPlugin. By
	 * default, this method returns null. If a specific InputPlugin wants to
	 * define Preferences, they must override this method.
	 * 
	 * NOTE: This method should never be called, and is just a template for how
	 * the getPreferences method should be defined. Static methods can't be
	 * overridden, and so this is just included as a template.
	 * 
	 * @param activity
	 *            The PreferenceActivity in which the preferences will appear.
	 * @return An array of the Preferences of this object.
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
	 * NOTE: Just like {@link InputPlugin#getPreferences(PreferenceActivity)},
	 * this is just a template, and cannot be overridden since it is static.
	 * 
	 * @return True if this plugin specifies its own preferences, false
	 *         otherwise.
	 */
	public static boolean hasPreferences() {
		return false;
	}

	protected boolean pluginEnabled;

	/**
	 * Sets the enabled-status of the plugin, starting or stopping it if
	 * necessary.
	 * 
	 * @param newPluginEnabledStatus
	 *            The new enabled status for the plugin.
	 */
	protected void changePluginEnabledStatus(
			final boolean newPluginEnabledStatus) {
		if (pluginEnabled && !newPluginEnabledStatus) {
			stopPlugin();
			pluginEnabled = newPluginEnabledStatus;
		} else if (!pluginEnabled && newPluginEnabledStatus) {
			pluginEnabled = newPluginEnabledStatus;
			startPlugin();
		}
	}

	/**
	 * @return True if the plugin is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return pluginEnabled;
	}

	/**
	 * Called when this InputPlugin is started.
	 */
	protected abstract void onPluginStart();

	/**
	 * Called when this InputPlugin is stopped.
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
	 * Starts the plugin.
	 */
	public final void startPlugin() {
		onPluginStart();
	}

	/**
	 * Stops the plugin.
	 */
	public final void stopPlugin() {
		onPluginStop();
	}

	/**
	 * Passes the given DataPacket to HSService.onDataReady().
	 * 
	 * @param packet
	 *            The given DataPacket to pass on to the output plugin streams.
	 */
	protected void write(final DataPacket packet) {
		HSService.onDataReady(packet, this);
	}

}
