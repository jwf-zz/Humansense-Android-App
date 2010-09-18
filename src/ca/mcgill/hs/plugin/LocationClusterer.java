package ca.mcgill.hs.plugin;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.location.GPSClusterer;
import ca.mcgill.hs.classifiers.location.WifiClusterer;
import ca.mcgill.hs.classifiers.location.WifiObservation;
import ca.mcgill.hs.plugin.WifiLogger.WifiPacket;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * Estimates a users' location based on wifi and gps signals. Does not try to
 * compute an absolute location in any coordinate space, but keeps track of
 * locations that users have visited on multiple occasions.
 */
public class LocationClusterer extends OutputPlugin {
	class WifiObservationConsumer implements Runnable {
		private final BlockingQueue<WifiObservation> queue;
		private boolean stopped;

		WifiObservationConsumer(final BlockingQueue<WifiObservation> q) {
			queue = q;
			stopped = false;
		}

		void consume(final WifiObservation observation) {
			if (observation == null) {
				return;
			}
			synchronized (wifiClusterer) {
				if (!stopped) {
					wifiClusterer.cluster(observation);
				}
			}
		}

		public void run() {
			try {
				while (!stopped) {
					consume(queue.poll(1L, TimeUnit.SECONDS));
				}
			} catch (final InterruptedException e) {
				// Do nothing.
			}
		}

		public void stop() {
			stopped = true;
		}
	}

	@SuppressWarnings("unused")
	private static final String TAG = "LocationClusterer";

	private static final String LOCATION_CLUSTERER_ENABLED_PREF = "locationClustererEnabledPref";

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				LOCATION_CLUSTERER_ENABLED_PREF,
				R.string.locationclusterer_enable_pref_label,
				R.string.locationclusterer_enable_pref_summary,
				R.string.locationclusterer_enable_pref_on,
				R.string.locationclusterer_enable_pref_off);
		return prefs;
	}

	public static boolean hasPreferences() {
		return true;
	}

	// The preference manager for this plugin.
	private final SharedPreferences prefs;

	// Keeps track of whether this plugin is enabled or not.
	private boolean pluginEnabled;
	private final Context context;

	private final BlockingQueue<WifiObservation> wifiObservationQueue = new LinkedBlockingQueue<WifiObservation>();

	private WifiObservationConsumer wifiObservationConsumer = null;

	private WifiClusterer wifiClusterer = null;

	private GPSClusterer gpsClusterer = null;

	public LocationClusterer(final Context context) {
		this.context = context;

		prefs = PreferenceFactory.getSharedPreferences();

	}

	@Override
	void onDataReceived(final DataPacket packet) {
		if (!pluginEnabled) {
			return;
		}
		if (packet.getDataPacketId() == WifiPacket.PACKET_ID) {
			final WifiPacket wifiPacket = (WifiPacket) packet;
			final double timestamp = wifiPacket.timestamp / 1000.0;
			final WifiObservation observation = new WifiObservation(timestamp,
					wifiPacket.neighbors);
			for (int i = 0; i < wifiPacket.neighbors; i++) {
				observation.addObservation(wifiPacket.BSSIDs[i].hashCode(),
						wifiPacket.levels[i]);
			}
			wifiObservationQueue.add(observation);
		}
	}

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs
				.getBoolean(LOCATION_CLUSTERER_ENABLED_PREF, false);
		if (!pluginEnabled) {
			return;
		}
		wifiClusterer = new WifiClusterer(context);
		gpsClusterer = new GPSClusterer(context
				.getDatabasePath("gpsclusters.db"));
		wifiObservationConsumer = new WifiObservationConsumer(
				wifiObservationQueue);
		new Thread(wifiObservationConsumer).start();
	}

	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		synchronized (wifiClusterer) {
			wifiObservationConsumer.stop();
			if (wifiClusterer != null) {
				wifiClusterer.close();
				wifiClusterer = null;
			}
			if (gpsClusterer != null) {
				gpsClusterer.close();
				wifiClusterer = null;
			}
		}
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				LOCATION_CLUSTERER_ENABLED_PREF, false);
		if (pluginEnabled && !pluginEnabledNew) {
			stopPlugin();
			pluginEnabled = pluginEnabledNew;
		} else if (!pluginEnabled && pluginEnabledNew) {
			pluginEnabled = pluginEnabledNew;
			startPlugin();
		}
	}

}
