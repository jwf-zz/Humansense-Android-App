package ca.mcgill.hs.classifiers.location;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
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
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		thisWidget = new ComponentName(context, LocationStatusWidget.class);
		manager = AppWidgetManager.getInstance(context);
		updateViews = new RemoteViews(context.getPackageName(),
				R.layout.location_status_appwidget);
	}
}
