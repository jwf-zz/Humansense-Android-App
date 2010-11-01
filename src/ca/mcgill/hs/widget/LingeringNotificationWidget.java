package ca.mcgill.hs.widget;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.plugin.TDEClassifierPlugin;

public class LingeringNotificationWidget extends AppWidgetProvider {
	public static class UpdateService extends Service {
		private static final SimpleDateFormat dfm = new SimpleDateFormat(
				"HH:mm:ss");
		private static final DecimalFormat formatter = new DecimalFormat(
				"0.###");

		private static final String TAG = "LingeringNotificationWidget.UpdateService";

		public RemoteViews buildUpdate(final Context context) {

			final TDEClassifierPlugin classifierPlugin = (TDEClassifierPlugin) PluginFactory
					.getOutputPlugin(TDEClassifierPlugin.class);
			RemoteViews views = null;

			if (classifierPlugin != null && classifierPlugin.isEnabled()) {
				final StringBuffer buf = new StringBuffer();
				final long timeLingering = classifierPlugin.getTimeLingering();
				final long timeMoving = classifierPlugin.getTimeMoving();
				final float[] cumulativeClassProbs = classifierPlugin
						.getCumulativeClassProbs();
				final List<String> modelNames = classifierPlugin
						.getModelNames();
				views = new RemoteViews(context.getPackageName(),
						R.layout.location_status_appwidget);

				buf.append("Lingering: " + timeLingering + "\nMoving: "
						+ timeMoving);
				float sum = 0.0f;
				for (int i = 0; i < cumulativeClassProbs.length; i++) {
					sum = sum + cumulativeClassProbs[i];
				}
				for (int i = 0; i < cumulativeClassProbs.length; i++) {
					buf
							.append("\nProb for "
									+ modelNames.get(i)
									+ ": "
									+ formatter
											.format((cumulativeClassProbs[i] / sum)));
				}
				views.setTextViewText(R.id.lingering_notification_text, buf
						.toString());

			} else {
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
					LingeringNotificationWidget.class);
			final AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, updateViews);
		}
	}

	private static final String TAG = "LingeringNotificationWidget";

	public static void updateText(final long timeLingering,
			final long timeMoving, final List<String> modelNames,
			final float[] cumulativeClassProbs) {
		Log.d(TAG, "updateText");
	}
}
