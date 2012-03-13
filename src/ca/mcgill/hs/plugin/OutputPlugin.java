/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.util.LinkedList;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.util.Log;

/**
 * Abstract class to be extended by all OutputPlugins.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public abstract class OutputPlugin implements Plugin, Runnable {

	/**
	 * Returns an array of Preference objects for the given OutputPlugin. By
	 * default, this method returns null. If a specific OutputPlugin wants to
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
	 * Returns whether or not this OutputPlugin has Preferences. By default,
	 * this method returns false. If a given OutputPlugin overrides the
	 * getPreferences(Context) method, they must also override this method to
	 * let it return true.
	 * 
	 * NOTE: Just like {@link OutputPlugin#getPreferences(PreferenceActivity)},
	 * this is just a template, and cannot be overridden since it is static.
	 * 
	 * @return True if this plugin specifies its own preferences, false
	 *         otherwise.
	 */
	public static boolean hasPreferences() {
		return false;
	}

	/**
	 * Queue for incoming DataPackets kept in case more than one arrives before
	 * the previous one can be handled.
	 */
	private final LinkedList<DataPacket> packetQueue = new LinkedList<DataPacket>();

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

	@Override
	public boolean isEnabled() {
		return pluginEnabled;
	}

	/**
	 * Called when a DataPacket is sent from an InputPlugin. Adds the new packet
	 * to the queue.
	 * 
	 * @param packet
	 *            The DataPacket that is ready to be received.
	 */
	public final synchronized void onDataReady(final DataPacket packet) {
		packetQueue.addLast(packet);
	}

	/**
	 * Called when there is data available for this plugin.
	 * 
	 * @param packet
	 *            The DataPacket that this plugin is receiving.
	 */
	abstract void onDataReceived(DataPacket packet);

	/**
	 * Called when this OutputPlugin is started.
	 */
	protected abstract void onPluginStart();

	/**
	 * Called when this OutputPlugin is stopped.
	 */
	protected abstract void onPluginStop();

	/**
	 * Signals the plugin that preferences have changed. OutputPlugin objects
	 * with preferences should override this method if something changes at
	 * runtime when preferences change. If this method is not overridden in a
	 * new plugin implementation, the preferences for that plugin will only
	 * update the next time the service is started.
	 */
	public void onPreferenceChanged() {
	}

	@Override
	public void run() {
		synchronized (packetQueue) {
			while (!packetQueue.isEmpty()) {
				try {
					final DataPacket packet = packetQueue.removeFirst();
					onDataReceived(packet);
				} catch (final java.util.NoSuchElementException e) {
					Log.e("OUTPUTPLUGIN Worker Thread", e);
					packetQueue.clear();
					break;
				}
			}
		}
	}

	/**
	 * Starts the plugin and calls onPluginStart().
	 */
	@Override
	public final void startPlugin() {
		onPluginStart();
	}

	/**
	 * Stops the plugin and calls onPluginStop().
	 */
	@Override
	public final void stopPlugin() {
		onPluginStop();
	}

}
