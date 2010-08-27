package ca.mcgill.hs.classifiers;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LingeringNotificationWidget extends AppWidgetProvider {
	private static ComponentName thisWidget = null;
	private static AppWidgetManager manager = null;

	private static RemoteViews updateViews = null;

	public static void updateText(final String text) {
		if (thisWidget != null && manager != null && updateViews != null) {
			updateViews.setTextViewText(R.id.lingering_notification_text, text);
			manager.updateAppWidget(thisWidget, updateViews);
		} else {
			Log.d("LingeringNotificationWidget", "Could not update widget.");
		}
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		thisWidget = new ComponentName(context,
				LingeringNotificationWidget.class);
		manager = AppWidgetManager.getInstance(context);
		updateViews = new RemoteViews(context.getPackageName(),
				R.layout.lingering_notification_appwidget);
	}

}
