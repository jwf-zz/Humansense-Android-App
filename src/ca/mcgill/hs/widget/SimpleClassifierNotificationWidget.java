/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
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
import android.widget.RemoteViews;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.plugin.TDEClassifierPlugin;
import ca.mcgill.hs.util.Log;

/**
 * A widget for displaying the output of the activity classifier and lingering
 * filter.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class SimpleClassifierNotificationWidget extends AppWidgetProvider {
	/**
	 * The service that runs in the background, periodically updating the widget
	 * text.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	public static class UpdateService extends Service {
		private static final SimpleDateFormat dfm = new SimpleDateFormat(
				"HH:mm:ss");
		private static final DecimalFormat formatter = new DecimalFormat(
				"0.###");

		private static final String TAG = "SimpleClassifierNotificationWidget.UpdateService";

		/**
		 * Builds the View that should be presented in the widget frame.
		 * 
		 * @param context
		 *            The application context, used for retrieving the data from
		 *            the appropriate plugin.
		 * @return A RemoteView, populated with the text, ready to be shown in
		 *         the widget frame.
		 */
		public RemoteViews buildUpdate(final Context context) {
			if (PluginFactory.getContext() == null) {
				PluginFactory.setContext(context);
			}
			final TDEClassifierPlugin classifierPlugin = (TDEClassifierPlugin) PluginFactory
					.getOutputPlugin(TDEClassifierPlugin.class);
			RemoteViews views = null;

			if (classifierPlugin != null && classifierPlugin.isEnabled()) {
				Log.d(TAG, "Checking for classification results.");
				// If we're classifying, gather the results.
				final StringBuffer buf = new StringBuffer();
				final long timeLingering = classifierPlugin.getTimeLingering();
				final long timeMoving = classifierPlugin.getTimeMoving();
				final float[] cumulativeClassProbs = classifierPlugin
						.getCumulativeClassProbs();
				final List<String> modelNames = classifierPlugin
						.getModelNames();
				views = new RemoteViews(context.getPackageName(),
						R.layout.simple_classifier_notification_appwidget);

				buf.append("Lingering: " + timeLingering + "\nMoving: "
						+ timeMoving);
				float sum = 0.0f;
				if (cumulativeClassProbs != null) {
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
				}
				views.setTextViewText(R.id.simple_classifier_notification_text,
						buf.toString());

			} else {
				// If we're not classifying, just show a default message.
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
					SimpleClassifierNotificationWidget.class);
			final AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, updateViews);
		}
	}

	private static final String TAG = "SimpleClassifierNotificationWidget";

	/**
	 * Just used for debugging, logs a message whenever it is called. All
	 * parameters are ignored, and to be honest I'm not sure why this method
	 * even exists.
	 * 
	 * @param timeLingering
	 * @param timeMoving
	 * @param modelNames
	 * @param cumulativeClassProbs
	 */
	public static void updateText(final long timeLingering,
			final long timeMoving, final List<String> modelNames,
			final float[] cumulativeClassProbs) {
		Log.d(TAG, "updateText");
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		// To prevent any ANR timeouts, we perform the update in a service
		context.startService(new Intent(context, UpdateService.class));
	}
}
