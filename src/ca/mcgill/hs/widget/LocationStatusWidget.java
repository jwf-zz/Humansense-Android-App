package ca.mcgill.hs.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.LocationClusterer;

public class LocationStatusWidget extends HSWidget {
	private static final String TAG = "LocationStatusWidget";
	private static final SimpleDateFormat dfm = new SimpleDateFormat("HH:mm:ss");
	private static final HashMap<Integer, ComponentName> widgetComponentNames = new HashMap<Integer, ComponentName>();
	private static final HashMap<Integer, AppWidgetManager> widgetManagers = new HashMap<Integer, AppWidgetManager>();
	private static final HashMap<Integer, RemoteViews> widgetViews = new HashMap<Integer, RemoteViews>();

	private static long mostRecentLocation = 0;
	private static SQLiteDatabase locationDatabase = null;

	private static Context context = null;

	private static String getClusterName(final long currentCluster) {
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
			Log.d(TAG, "Found a location name '" + clusterName + "' for id "
					+ currentCluster + ".");
		} else {
			Log.d(TAG, "Could not find a location name for id "
					+ currentCluster + ".");
		}
		cursor.close();
		return clusterName;
	}

	public static long getLastLocationId() {
		return mostRecentLocation;
	}

	public static void updateWidget(final int clusteredPoints,
			final int poolSize, final long currentCluster,
			final boolean currentlyMoving) {
		Log.d(TAG, "updateWidget");
		mostRecentLocation = currentCluster;
		final String clusterName = getClusterName(currentCluster);
		final Set<Integer> keys = widgetComponentNames.keySet();
		Log.d(TAG, "Updating " + keys.size() + " widgets.");
		if (keys.size() == 0 || context == null) {
			return;
		}
		final StringBuffer buf = new StringBuffer();
		buf.append("Update at: "
				+ dfm.format(new Date(System.currentTimeMillis())) + "\n");
		buf.append("Clustered " + clusteredPoints + " of " + poolSize
				+ " points.\n");
		buf.append("Currently in location: " + clusterName + "\n");
		final String text = buf.toString();
		for (final Integer key : keys) {
			if (currentlyMoving) {
				widgetViews.get(key).setInt(R.id.location_status_text,
						"setBackgroundResource",
						R.drawable.location_status_widget_bg_moving);
			} else {
				widgetViews.get(key).setInt(R.id.location_status_text,
						"setBackgroundResource",
						R.drawable.location_status_widget_bg_stationary);
			}
			widgetViews.get(key).setTextViewText(R.id.location_status_text,
					text);
			widgetViews.get(key).setTextColor(R.id.location_status_text,
					Color.WHITE);
			widgetManagers.get(key).updateAppWidget(key, widgetViews.get(key));
		}
	}

	@Override
	protected ComponentName getComponentName(final Context context) {
		return new ComponentName(context, LocationStatusWidget.class);
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.location_status_appwidget;
	}

	@Override
	protected Map<Integer, ComponentName> getWidgetComponentNames() {
		return widgetComponentNames;
	}

	@Override
	protected Map<Integer, AppWidgetManager> getWidgetManagers() {
		return widgetManagers;
	}

	@Override
	protected Map<Integer, RemoteViews> getWidgetViews() {
		return widgetViews;
	}

	@Override
	protected void onStart(final int appWidgetId, final Context context) {
		Log.d(TAG, "onStart");
		LocationStatusWidget.context = context;
		final Intent locationLabeler = new Intent(context
				.getApplicationContext(), LocationLabelerDialog.class);
		final PendingIntent pendingIntent = PendingIntent.getActivity(context,
				0, locationLabeler, Intent.FLAG_ACTIVITY_NEW_TASK);
		final LocationClusterer.LocationDictionaryOpenHelper dbOpener = new LocationClusterer.LocationDictionaryOpenHelper(
				context);
		locationDatabase = dbOpener.getReadableDatabase();
		widgetViews.get(appWidgetId).setOnClickPendingIntent(
				R.id.location_status_text, pendingIntent);
	}

	@Override
	protected void onStop(final int appWidgetId) {
		Log.d(TAG, "onStop");
	}
}
