/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.classifiers.location.GPSClusterer;
import ca.mcgill.hs.classifiers.location.WifiClusterer;
import ca.mcgill.hs.classifiers.location.WifiObservation;
import ca.mcgill.hs.plugin.WifiLogger.WifiPacket;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.widget.LocationStatusWidget;

/**
 * Estimates a users' location based on wifi and gps signals. Does not try to
 * compute an absolute location in any coordinate space, but keeps track of
 * locations that users have visited on multiple occasions.
 */
public class LocationClusterer extends OutputPlugin {
	public static class LocationDictionaryOpenHelper extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "locations.db";
		private static final String DICTIONARY_TABLE_NAME = "locations";
		private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE "
				+ DICTIONARY_TABLE_NAME + " (" + "id INTEGER PRIMARY KEY, "
				+ "location_name" + " TEXT);";

		public LocationDictionaryOpenHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(DICTIONARY_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			// Nothing to do yet.
		}
	}

	class WifiObservationConsumer implements Runnable {
		private final BlockingQueue<WifiObservation> queue;
		private boolean stopped;
		private final GPSLogger gpsLogger = (GPSLogger) PluginFactory
				.getInputPlugin(GPSLogger.class);

		WifiObservationConsumer(final BlockingQueue<WifiObservation> q) {
			queue = q;
			stopped = false;
		}

		void consume(final WifiObservation observation) {
			if (observation == null) {
				return;
			}
			synchronized (queue) {
				if (!stopped) {
					wifiClusterer.cluster(observation);
					if (manageGPS) {
						final boolean currentlyMoving = wifiClusterer
								.isMoving();
						if (currentlyMoving) {
							Log.d(PLUGIN_NAME, "User is currently moving.");
						} else {
							Log.d(PLUGIN_NAME, "User is currently stationary.");
						}
						if (previouslyMoving && !currentlyMoving) {
							// User has stopped moving, disable GPS
							Log
									.d(PLUGIN_NAME,
											"User went from moving to stationary, disabling GPS");
							gpsLogger.disableAfterNextScan();
						} else if (!previouslyMoving && currentlyMoving) {
							// User has started moving, enable GPS
							Log
									.d(PLUGIN_NAME,
											"User went from stationary to moving, enabling GPS");
							gpsLogger.enable();
						}
						previouslyMoving = currentlyMoving;
					}
					context.startService(new Intent(context,
							LocationStatusWidget.UpdateService.class));
				}
			}
		}

		@Override
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

	private static final String PLUGIN_NAME = "LocationClusterer";
	private static final String LOCATION_CLUSTERER_ENABLED_PREF = "locationClustererEnabledPref";

	private static final String LOCATION_CLUSTERER_USE_RESULTS_TO_MANAGE_GPS = "locationClustererManageGPS";

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[2];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				LOCATION_CLUSTERER_ENABLED_PREF,
				R.string.locationclusterer_enable_pref_label,
				R.string.locationclusterer_enable_pref_summary,
				R.string.locationclusterer_enable_pref_on,
				R.string.locationclusterer_enable_pref_off, false);

		prefs[1] = PreferenceFactory.getCheckBoxPreference(activity,
				LOCATION_CLUSTERER_USE_RESULTS_TO_MANAGE_GPS,
				R.string.locationclusterer_manage_gps_pref_label,
				R.string.locationclusterer_manage_gps_pref_summary,
				R.string.locationclusterer_manage_gps_pref_on,
				R.string.locationclusterer_manage_gps_pref_off, false);

		return prefs;
	}

	public static boolean hasPreferences() {
		return true;
	}

	private boolean previouslyMoving = true;

	private boolean manageGPS;

	// The preference manager for this plugin.
	private final SharedPreferences prefs;

	private final Context context;

	private final BlockingQueue<WifiObservation> wifiObservationQueue = new LinkedBlockingQueue<WifiObservation>();

	private WifiObservationConsumer wifiObservationConsumer = null;

	private WifiClusterer wifiClusterer = null;

	private GPSClusterer gpsClusterer = null;

	public LocationClusterer(final Context context) {
		this.context = context;

		prefs = PreferenceFactory.getSharedPreferences();
	}

	public long getCurrentCluster() {
		if (wifiClusterer != null) {
			return wifiClusterer.getCurrentCluster();
		} else {
			return -1;
		}

	}

	public boolean isMoving() {
		if (wifiClusterer != null) {
			return wifiClusterer.isMoving();
		} else {
			return false;
		}
	}

	@Override
	void onDataReceived(final DataPacket packet) {
		if (!pluginEnabled) {
			return;
		}
		if (packet.getDataPacketId() == WifiPacket.PACKET_ID) {
			final WifiPacket wifiPacket = (WifiPacket) packet;
			final double timestamp = wifiPacket.timestamp / 1000.0;
			final int numAccessPoints = wifiPacket.numAccessPoints;
			if (numAccessPoints < 1) {
				// Only cluster if there is at least one access point in range.
				return;
			}
			final WifiObservation observation = new WifiObservation(timestamp,
					numAccessPoints);
			final String[] bssids = wifiPacket.BSSIDs;
			final int[] signalStrengths = wifiPacket.signalStrengths;
			for (int i = 0; i < numAccessPoints; i++) {
				observation.addObservation(bssids[i].hashCode(),
						signalStrengths[i]);
			}
			wifiObservationQueue.add(observation);
		}
	}

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs
				.getBoolean(LOCATION_CLUSTERER_ENABLED_PREF, false);
		manageGPS = prefs.getBoolean(
				LOCATION_CLUSTERER_USE_RESULTS_TO_MANAGE_GPS, false);

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
		synchronized (wifiObservationQueue) {
			if (wifiObservationConsumer != null) {
				wifiObservationConsumer.stop();
				wifiObservationConsumer = null;
			}
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
		manageGPS = prefs.getBoolean(
				LOCATION_CLUSTERER_USE_RESULTS_TO_MANAGE_GPS, false);
		super.changePluginEnabledStatus(pluginEnabledNew);
	}
}
