package ca.mcgill.hs.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LocationLabelerWidget extends AppWidgetProvider {
	private static final String TAG = "LocationLabelerWidget";
	static final int GET_LOCATION_LABEL = 0;

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		final int N = appWidgetIds.length;
		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < N; i++) {
			final int appWidgetId = appWidgetIds[i];
			// Create an Intent to launch ExampleActivity
			final Intent intent = new Intent(context,
					LocationLabelerDialog.class);
			final PendingIntent pendingIntent = PendingIntent.getActivity(
					context, 0, intent, 0);

			// Setup the onClick Handler
			final RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.location_labeler_appwidget);
			views.setOnClickPendingIntent(R.id.location_labeler_widget_text,
					pendingIntent);

			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}
