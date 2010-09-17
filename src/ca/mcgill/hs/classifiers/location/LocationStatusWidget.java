package ca.mcgill.hs.classifiers.location;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LocationStatusWidget extends android.appwidget.AppWidgetProvider {
	private static ComponentName thisWidget = null;
	private static AppWidgetManager manager = null;
	private static RemoteViews updateViews = null;
	private static final String TAG = "LocationStatusWidget";

	public static void updateText(final String text) {
		if (thisWidget != null && manager != null && updateViews != null) {
			updateViews.setTextViewText(R.id.location_status_text, text);
			manager.updateAppWidget(thisWidget, updateViews);
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
	public void onDisabled(final Context context) {
		// When the first widget is created, stop listening for the
		// TIMEZONE_CHANGED and
		// TIME_CHANGED broadcasts.
		Log.d(TAG, "onDisabled");
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("ca.mcgill.hs",
				".location.LocationStatusWidget"),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
	}

	@Override
	public void onEnabled(final Context context) {
		Log.d(TAG, "onEnabled");
		// When the first widget is created, register for the TIMEZONE_CHANGED
		// and TIME_CHANGED
		// broadcasts. We don't want to be listening for these if nobody has our
		// widget active.
		// This setting is sticky across reboots, but that doesn't matter,
		// because this will
		// be called after boot if there is a widget instance for this provider.
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("ca.mcgill.hs",
				".location.LocationStatusWidget"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		thisWidget = new ComponentName(context, LocationStatusWidget.class);
		manager = AppWidgetManager.getInstance(context);
		updateViews = new RemoteViews(context.getPackageName(),
				R.layout.location_status_appwidget);
	}

}
