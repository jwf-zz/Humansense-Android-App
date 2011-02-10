/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.serv;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.DataPacket;
import ca.mcgill.hs.plugin.InputPlugin;
import ca.mcgill.hs.plugin.OutputPlugin;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

/**
 * The main service that runs in the background and manages the communication
 * between plugins.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class HSService extends Service {

	private static final String TAG = "HSService";

	private static boolean isRunning;

	// Lists of all active input and output plugins.
	private static final LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	private static final LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();

	// List of the plugin classes that are available.
	private static final Class<? extends InputPlugin>[] inputPluginClasses = PluginFactory
			.getInputPluginClassList();
	private static final Class<? extends OutputPlugin>[] outputPluginClasses = PluginFactory
			.getOutputPluginClassList();

	// Threadpool used to handle output plugin processing.
	private static ExecutorService tpe;

	// This is a BroadcastReceiver in order to signal to the plugins that a
	// preference has changed.
	private static final BroadcastReceiver prefReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			Log.d(TAG, "Received Preferences Changed Intent");
			preferencesChangedIntentReceived();
		}
	};

	public static Class<? extends InputPlugin>[] getInputPluginClasses() {
		return inputPluginClasses;
	}

	public static LinkedList<InputPlugin> getInputPlugins() {
		return inputPluginList;
	}

	public static Class<? extends OutputPlugin>[] getOutputPluginClasses() {
		return outputPluginClasses;
	}

	public static LinkedList<OutputPlugin> getOutputPlugins() {
		return outputPluginList;
	}

	/**
	 * Populates the list of input plugins.
	 */
	public static void initializeInputPlugins() {
		inputPluginList.clear();
		for (final Class<? extends InputPlugin> plugin : inputPluginClasses) {
			inputPluginList.add(PluginFactory.getInputPlugin(plugin));
		}
	}

	/**
	 * Populates the list of output plugins.
	 */
	public static void initializeOutputPlugins() {
		outputPluginList.clear();
		for (final Class<? extends OutputPlugin> plugin : outputPluginClasses) {
			outputPluginList.add(PluginFactory.getOutputPlugin(plugin));
		}
	}

	/**
	 * Returns a boolean indicating if the service is running or not.
	 * 
	 * @return True if the service is running and false otherwise.
	 */
	public static boolean isRunning() {
		return isRunning;
	}

	/**
	 * Called when there is a DataPacket available from an InputPlugin.
	 * 
	 * @param packet
	 *            The DataPacket that is ready to be received.
	 * @param source
	 *            The InputPlugin that created the DataPacket.
	 */
	public static void onDataReady(final DataPacket packet,
			final InputPlugin source) {
		if (isRunning) {
			for (final OutputPlugin plugin : outputPluginList) {
				plugin.onDataReady(packet.clone());
				tpe.execute(plugin);
			}
		}
	}

	/**
	 * This method is called whenever a preference has been changed. It signals
	 * all plugins that a preference has changed.
	 */
	private static void preferencesChangedIntentReceived() {
		for (final InputPlugin p : inputPluginList) {
			p.onPreferenceChanged();
		}
		for (final OutputPlugin p : outputPluginList) {
			p.onPreferenceChanged();
		}
	}

	/**
	 * Generates a notification in the status bar alerting the user that the
	 * service is running in the background.
	 * 
	 * @return
	 */
	private Notification getServiceStartedNotification() {
		final int icon = R.drawable.notification_icon;
		final CharSequence tickerText = getResources().getString(
				R.string.started_notification_text);
		final long when = System.currentTimeMillis();
		final Context context = getApplicationContext();
		final CharSequence contentTitle = getResources().getString(
				R.string.started_notification_title);
		final CharSequence contentText = getResources().getString(
				R.string.started_notification_content);

		final Intent notificationIntent = new Intent(this, HSService.class);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		final Notification notification = new Notification(icon, tickerText,
				when);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		return notification;
	}

	@Override
	public IBinder onBind(final Intent intent) {
		// Unused
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isRunning = false;
		// Stop listening for preference updates.
		getApplicationContext().unregisterReceiver(prefReceiver);

		// Stop running as a foreground service. This also removes the
		// notification from the status bar.
		stopForeground(true);

		Log.d(TAG, "Sending stop signal to " + inputPluginList.size()
				+ " input plugins.");
		for (final InputPlugin plugin : inputPluginList) {
			plugin.stopPlugin();
		}
		Log.d(TAG, "Sending stop signal to " + outputPluginList.size()
				+ " output plugins.");

		for (final OutputPlugin plugin : outputPluginList) {
			plugin.stopPlugin();
		}

		// Close the threadpool.
		shutdownAndAwaitTermination();
		HSAndroid.updateButton();
	}

	@Override
	public void onStart(final Intent intent, final int startId) {
		super.onStart(intent, startId);
		if (isRunning) {
			return;
		}

		// Start as foreground service so we don't get killed on low memory.
		final int notification_id = getResources().getString(
				R.string.started_notification_text).hashCode();
		startForeground(notification_id, getServiceStartedNotification());

		// Create a new thread pool for handling output plugins
		tpe = Executors.newCachedThreadPool();

		Log.d(TAG, "Sending start signal to " + outputPluginList.size()
				+ " output plugins.");
		for (final OutputPlugin plugin : outputPluginList) {
			plugin.startPlugin();
		}
		Log.d(TAG, "Sending start signal to " + inputPluginList.size()
				+ " input plugins.");
		for (final InputPlugin plugin : inputPluginList) {
			plugin.startPlugin();
		}

		// Register the receiver for when the preferences change.
		getApplicationContext().registerReceiver(prefReceiver,
				new IntentFilter(PreferenceFactory.PREFERENCES_CHANGED_INTENT));

		isRunning = true;
		HSAndroid.updateButton();
	}

	/**
	 * Cleanly close the threadpool.
	 */
	private void shutdownAndAwaitTermination() {
		tpe.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!tpe.awaitTermination(60, TimeUnit.SECONDS)) {
				tpe.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being canceled
				if (!tpe.awaitTermination(60, TimeUnit.SECONDS)) {
					Log.e(TAG, "Pool did not terminate");
				}
			}
		} catch (final InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			tpe.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
