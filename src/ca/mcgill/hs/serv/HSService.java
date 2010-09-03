package ca.mcgill.hs.serv;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import ca.mcgill.hs.plugin.DataPacket;
import ca.mcgill.hs.plugin.InputPlugin;
import ca.mcgill.hs.plugin.OutputPlugin;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.util.PreferenceFactory;

public class HSService extends Service {

	private static boolean isRunning;

	// If true then performance timing information will be logged
	private static final boolean PERF_COUNTERS = false;

	// Lists of the plugins currently enabled.
	private static final LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();

	private static final LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	// Some variables for calculating performance metrics
	private static long timeSpentInOnDataReady = 0L;

	private static int numCallsToOnDataReady = 0;
	// A simple static array of the input plugin class names.
	public static final Class<? extends InputPlugin>[] inputPluginClasses = PluginFactory
			.getInputPluginClassList();

	// A simple static array of the output plugin class names.
	public static final Class<? extends OutputPlugin>[] outputPluginClasses = PluginFactory
			.getOutputPluginClassList();

	// ExecutorService
	private static final ExecutorService tpe = Executors.newCachedThreadPool();

	// This is a BroadcastReceiver in order to signal to the plugins that a
	// preference has changed.
	private static final BroadcastReceiver prefReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			preferencesChangedIntentReceived();
		}
	};

	/**
	 * Populates the list of output plugins.
	 */
	private static void addOutputPlugins() {
		for (final Class<? extends OutputPlugin> plugin : outputPluginClasses) {
			outputPluginList.add(PluginFactory.getOutputPlugin(plugin));
		}
	}

	/**
	 * Returns a boolean indicating if the service is running or not.
	 * 
	 * @return true if the service is running and false otherwise.
	 */
	public static boolean isRunning() {
		return isRunning;
	}

	/**
	 * Called when there is a DataPacket available from an InputPlugin.
	 * 
	 * @param dp
	 *            the DataPacket that is ready to be received.
	 * @param source
	 *            the InputPlugin that created the DataPacket.
	 */
	public static void onDataReady(final DataPacket dp, final InputPlugin source) {
		final long currentTime;
		if (PERF_COUNTERS) {
			currentTime = System.currentTimeMillis();
		}
		for (final OutputPlugin op : outputPluginList) {
			op.onDataReady(dp.clone());
			tpe.execute(op);
		}
		if (PERF_COUNTERS) {
			timeSpentInOnDataReady += (System.currentTimeMillis() - currentTime);
			numCallsToOnDataReady += 1;
			if (numCallsToOnDataReady % 1000 == 0) {
				final float avgTime = (float) timeSpentInOnDataReady
						/ (float) numCallsToOnDataReady;
				Log.d("PERFORMANCE:", "Average time for onDataReady is "
						+ avgTime + " milliseconds");
				timeSpentInOnDataReady = 0L;
				numCallsToOnDataReady = 0;
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
	 * Populates the list of input plugins.
	 */
	private void addInputPlugins() {
		for (final Class<? extends InputPlugin> plugin : inputPluginClasses) {
			inputPluginList.add(PluginFactory.getInputPlugin(plugin));
		}
	}

	@Override
	// Unused
	public IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * Called when the service is started. Creates the service.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
	}

	/**
	 * Called when the service is stopped. Also stops all plugins.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		for (final InputPlugin plugin : inputPluginList) {
			plugin.stopPlugin();
		}
		for (final OutputPlugin plugin : outputPluginList) {
			plugin.stopPlugin();
		}

		inputPluginList.clear();
		outputPluginList.clear();

		isRunning = false;
	}

	/**
	 * Called automatically when onCreate() is called. Initialises the service
	 * and associated plug-ins and starts the service.
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		if (isRunning) {
			return;
		}
		super.onStart(intent, startId);

		PluginFactory.setContext(getApplicationContext());

		// Register the receiver for when the preferences change.
		getApplicationContext().registerReceiver(prefReceiver,
				new IntentFilter(PreferenceFactory.PREFERENCES_CHANGED_INTENT));

		// Instantiate input plugins.
		addInputPlugins();

		// Instantiate output plugins
		addOutputPlugins();

		// Start input plugins.
		for (final InputPlugin plugin : inputPluginList) {
			plugin.startPlugin();
		}

		// Start output plugins.
		for (final OutputPlugin plugin : outputPluginList) {
			plugin.startPlugin();
		}

		isRunning = true;

		// Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
}
