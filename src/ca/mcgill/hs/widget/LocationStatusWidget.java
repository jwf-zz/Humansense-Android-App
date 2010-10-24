package ca.mcgill.hs.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LocationStatusWidget extends HSWidget {
	private static final String TAG = "LocationStatusWidget";
	private static final SimpleDateFormat dfm = new SimpleDateFormat("HH:mm:ss");
	private static final HashMap<Integer, ComponentName> widgetComponentNames = new HashMap<Integer, ComponentName>();
	private static final HashMap<Integer, AppWidgetManager> widgetManagers = new HashMap<Integer, AppWidgetManager>();
	private static final HashMap<Integer, RemoteViews> widgetViews = new HashMap<Integer, RemoteViews>();

	private static long mostRecentLocation = 0;

	public static long getLastLocationId() {
		return mostRecentLocation;
	}

	public static void updateWidget(final int clusteredPoints,
			final int poolSize, final long currentCluster,
			final boolean currentlyMoving) {
		mostRecentLocation = currentCluster;
		Log.d(TAG, "updateWidget");
		final StringBuffer buf = new StringBuffer();
		buf.append("Update at: "
				+ dfm.format(new Date(System.currentTimeMillis())) + "\n");
		buf.append("Clustered " + clusteredPoints + " of " + poolSize
				+ " points.\n");
		buf.append("Currently in location: " + currentCluster + "\n");
		final String text = buf.toString();
		for (final Integer key : widgetComponentNames.keySet()) {
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
		final Intent locationLabeler = new Intent(context
				.getApplicationContext(), LocationLabelerDialog.class);
		final PendingIntent pendingIntent = PendingIntent.getActivity(context,
				0, locationLabeler, Intent.FLAG_ACTIVITY_NEW_TASK);
		widgetViews.get(appWidgetId).setOnClickPendingIntent(
				R.id.location_status_text, pendingIntent);
	}

	@Override
	protected void onStop(final int appWidgetId) {
	}
}
