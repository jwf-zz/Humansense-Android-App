package ca.mcgill.hs.widget;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import ca.mcgill.hs.R;

public class LingeringNotificationWidget extends HSWidget {
	private static final String TAG = "LingeringNotificationWidget";
	private static final DecimalFormat formatter = new DecimalFormat("0.###");
	private static final HashMap<Integer, ComponentName> widgetComponentNames = new HashMap<Integer, ComponentName>();
	private static final HashMap<Integer, AppWidgetManager> widgetManagers = new HashMap<Integer, AppWidgetManager>();
	private static final HashMap<Integer, RemoteViews> widgetViews = new HashMap<Integer, RemoteViews>();

	public static void updateText(final long timeLingering,
			final long timeMoving, final List<String> modelNames,
			final float[] cumulativeClassProbs) {
		Log.d(TAG, "updateText");
		final StringBuffer buf = new StringBuffer();
		buf.append("Lingering: " + timeLingering + "\nMoving: " + timeMoving);
		float sum = 0.0f;
		for (int i = 0; i < cumulativeClassProbs.length; i++) {
			sum = sum + cumulativeClassProbs[i];
		}
		for (int i = 0; i < cumulativeClassProbs.length; i++) {
			buf.append("\nProb for " + modelNames.get(i) + ": "
					+ formatter.format((cumulativeClassProbs[i] / sum)));
		}
		for (final Integer key : widgetComponentNames.keySet()) {
			widgetViews.get(key).setTextViewText(
					R.id.lingering_notification_text, buf.toString());
			widgetManagers.get(key).updateAppWidget(key, widgetViews.get(key));
		}
	}

	@Override
	protected ComponentName getComponentName(final Context context) {
		return new ComponentName(context, LingeringNotificationWidget.class);
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.lingering_notification_appwidget;
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
	protected void onStart(final int appWidgetId, final Context contex) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStop(final int appWidgetId) {
		// TODO Auto-generated method stub

	}
}
