package ca.mcgill.hs.classifiers.location;

import java.util.HashMap;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LocationStatusWidget extends android.appwidget.AppWidgetProvider {
	private static HashMap<Integer, ComponentName> thisWidget = new HashMap<Integer, ComponentName>();
	private static HashMap<Integer, AppWidgetManager> manager = new HashMap<Integer, AppWidgetManager>();
	private static HashMap<Integer, RemoteViews> updateViews = new HashMap<Integer, RemoteViews>();
	private static final String TAG = "LocationStatusWidget";

	public static void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager, final int appWidgetId) {
		Log.d(TAG, "updateAppWidget");
		// For each widget that needs an update, get the text that we should
		// display:
		// - Create a RemoteViews object for it
		// - Set the text in the RemoteViews object
		// - Tell the AppWidgetManager to show that views object for the widget.
		thisWidget.put(appWidgetId, new ComponentName(context,
				LocationStatusWidget.class));
		manager.put(appWidgetId, AppWidgetManager.getInstance(context));
		updateViews.put(appWidgetId, new RemoteViews(context.getPackageName(),
				R.layout.location_status_appwidget));
		appWidgetManager.updateAppWidget(appWidgetId, updateViews
				.get(appWidgetId));
	}

	public static void updateText(final String text, final boolean moving) {
		Log.d(TAG, "updateText");
		if (thisWidget != null && manager != null && updateViews != null) {
			for (final Integer key : thisWidget.keySet()) {
				if (moving) {
					updateViews.get(key).setInt(R.id.location_status_text,
							"setBackgroundResource",
							R.drawable.location_status_widget_bg_moving);
				} else {
					updateViews.get(key).setInt(R.id.location_status_text,
							"setBackgroundResource",
							R.drawable.location_status_widget_bg_stationary);
				}
				updateViews.get(key).setTextViewText(R.id.location_status_text,
						text);
				updateViews.get(key).setTextColor(R.id.location_status_text,
						Color.WHITE);
				manager.get(key).updateAppWidget(key, updateViews.get(key));

			}
		} else {
			if (thisWidget == null) {
				Log.d(TAG, "thisWidget is null");
			}
			if (manager == null) {
				Log.d(TAG, "manager is null");
			}
			if (updateViews == null) {
				Log.d(TAG, "updateViews is null");
			}
			Log.d(TAG, "Could not update widget.");
		}
	}

	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		Log.d(TAG, "onDeleted");
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			thisWidget.remove(appWidgetIds[i]);
			manager.remove(appWidgetIds[i]);
			updateViews.remove(appWidgetIds[i]);
		}
	}

	@Override
	public void onDisabled(final Context context) {
		Log.d(TAG, "onDisabled");
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("ca.mcgill.hs",
				".location.LocationStatusWidget"),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onEnabled(final Context context) {
		Log.d(TAG, "onEnabled");
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("ca.mcgill.hs",
				".location.LocationStatusWidget"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			final int appWidgetId = appWidgetIds[i];
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}
}
