/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.widget;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.IBinder;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.LocationClusterer;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.util.Log;

/**
 * Displays the current location, and the motion status.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class LocationStatusWidget extends AppWidgetProvider {
	/**
	 * Service that runs in the background and polls the location estimation
	 * plugin for results.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	public static class UpdateService extends Service {
		private static final SimpleDateFormat dfm = new SimpleDateFormat(
				"HH:mm:ss");

		private static final String TAG = "LocationStatusWidget.UpdateService";

		/**
		 * Retrieves a cluster name from the database.
		 * 
		 * @param currentCluster
		 *            The cluster id.
		 * @param context
		 *            The application context.
		 * @return The cluster name.
		 */
		private static String getClusterName(final long currentCluster,
				final Context context) {
			if (currentCluster < 0) {
				return context
						.getString(R.string.location_widget_unknown_location);
			}
			String clusterName = Long.toString(currentCluster);
			if (locationDatabase == null) {
				if (context == null) {
					return clusterName;
				}
				final LocationClusterer.LocationDictionaryOpenHelper dbOpener = new LocationClusterer.LocationDictionaryOpenHelper(
						context);
				locationDatabase = dbOpener.getReadableDatabase();
			}
			final Cursor cursor = locationDatabase.query("locations",
					new String[] { "location_name" }, "id=?",
					new String[] { clusterName }, null, null, null, "1");
			if (cursor.moveToFirst()) {
				clusterName = cursor.getString(0);
				Log.d(TAG, "Found a location name '" + clusterName
						+ "' for id " + currentCluster + ".");
			} else {
				Log.d(TAG, "Could not find a location name for id "
						+ currentCluster + ".");
			}
			cursor.close();
			return clusterName;
		}

		/**
		 * Builds the View that should be presented in the widget frame.
		 * 
		 * @param context
		 *            The application context, required for accessing the
		 *            locations database.
		 * @return A RemoteView object populated with the text, and with the
		 *         background colour indicating the motion status, ready to be
		 *         displayed in the widget frame.
		 */
		public RemoteViews buildUpdate(final Context context) {
			if (PluginFactory.getContext() == null) {
				PluginFactory.setContext(context);
			}
			final LocationClusterer locationPlugin = (LocationClusterer) PluginFactory
					.getOutputPlugin(LocationClusterer.class);
			boolean moving = false;
			RemoteViews views = null;

			if (locationPlugin != null && locationPlugin.isEnabled()) {
				// If the location estimation plugin is active, retrieve the
				// results.
				moving = locationPlugin.isMoving();
				mostRecentClusterId = locationPlugin.getCurrentCluster();
				final String clusterName = getClusterName(mostRecentClusterId,
						context);
				final StringBuffer buf = new StringBuffer();
				buf.append("Update at: "
						+ dfm.format(new Date(System.currentTimeMillis()))
						+ "\n");

				if (moving) {
					buf.append(context
							.getString(R.string.location_widget_moving_label)
							+ "\n");
				} else {
					buf
							.append(context
									.getString(R.string.location_widget_location_prefix)
									+ " " + clusterName + "\n");
				}
				final String text = buf.toString();

				views = new RemoteViews(context.getPackageName(),
						R.layout.location_status_appwidget);
				if (moving) {
					views.setInt(R.id.location_status_text,
							"setBackgroundResource",
							R.drawable.location_status_widget_bg_moving);
				} else {
					views.setInt(R.id.location_status_text,
							"setBackgroundResource",
							R.drawable.location_status_widget_bg_stationary);
					if (mostRecentClusterId > 0) {
						final Intent locationLabeler = new Intent(context
								.getApplicationContext(),
								LocationLabelerDialog.class);
						final PendingIntent pendingIntent = PendingIntent
								.getActivity(context, 0, locationLabeler, 0);
						views.setOnClickPendingIntent(
								R.id.location_status_text, pendingIntent);
					}
				}
				views.setTextViewText(R.id.location_status_text, text);
				views.setTextColor(R.id.location_status_text, Color.WHITE);

			} else {
				// If the location plugin is not enabled, just display some
				// default text.
				views = new RemoteViews(context.getPackageName(),
						R.layout.widget_message);
				views.setTextViewText(R.id.message, context
						.getString(R.string.widget_error));
			}
			return views;
		}

		@Override
		public IBinder onBind(final Intent intent) {
			return null;
		}

		@Override
		public void onStart(final Intent intent, final int startId) {
			Log.d(TAG, "onStart");
			// Build the widget update for today
			final RemoteViews updateViews = buildUpdate(this);

			// Push update for this widget to the home screen
			final ComponentName thisWidget = new ComponentName(this,
					LocationStatusWidget.class);
			final AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, updateViews);
			Log.d(TAG, "Done onStart");
		}
	}

	private static SQLiteDatabase locationDatabase = null;
	private static final String TAG = "LocationStatusWidget";
	private static long mostRecentClusterId = -1;

	public static long getMostRecentClusterId() {
		return mostRecentClusterId;
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		// To prevent any ANR timeouts, we perform the update in a service
		context.startService(new Intent(context, UpdateService.class));
	}
}
