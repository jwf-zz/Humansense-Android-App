package ca.mcgill.hs.plugin;

import java.util.LinkedList;

import android.preference.Preference;

/**
 * Abstract class to be extended by all OutputPlugins. Provides an interface for
 * using OutputPlugins.
 */
public abstract class OutputPlugin implements Plugin, Runnable {

	/**
	 * This method returns an array of Preference objects for the given
	 * OutputPlugin. By default, this method returns null. If a specific
	 * OutputPlugin wants to define Preferences, they must override this method.
	 * 
	 * @return An array of the Preferences of this object.
	 */
	public static Preference[] getPreferences() {
		return null;
	}

	/**
	 * Returns whether or not this OutputPlugin has Preferences. By default,
	 * this method returns false. If a given OutputPlugin overrides the
	 * getPreferences(Context) method, they must also override this method to
	 * let it return true.
	 * 
	 * @return Whether or not this OutputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return false;
	}

	// Waiting list for incoming DataPackets kept in case more than one arrives
	// before the previous one can be handled.
	private final LinkedList<DataPacket> packetList = new LinkedList<DataPacket>();

	protected boolean pluginEnabled;

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
	 * Called when a DataPacket is sent from an InputPlugin. Adds the DataPacket
	 * that is now ready to dpList.
	 * 
	 * @param packet
	 *            The DataPacket that is ready to be received.
	 */
	public final synchronized void onDataReady(final DataPacket packet) {
		packetList.addLast(packet);
	}

	/**
	 * Called when there is data available for this plugin.
	 * 
	 * @param packet
	 *            The DataPacket that this plugin is receiving.
	 */
	abstract void onDataReceived(DataPacket packet);

	/**
	 * Called when this OutputPlugin is started. This method is meant to be
	 * overridden.
	 */
	protected abstract void onPluginStart();

	/**
	 * Called when this OutputPlugin is stopped. This method is meant to be
	 * overridden.
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

	/**
	 * Used by the ThreadPool to continuously retrieved DataPackets from dpList.
	 * The DataPacket are passed to onDataReceived() one at a time for as long
	 * as this plugin is running and dpList is not empty.
	 */
	public synchronized void run() {
		while (!packetList.isEmpty()) {
			final DataPacket packet = packetList.removeFirst();
			onDataReceived(packet);
		}
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

}
