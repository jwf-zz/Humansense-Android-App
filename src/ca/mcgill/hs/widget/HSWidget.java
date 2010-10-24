package ca.mcgill.hs.widget;

import java.util.Map;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.RemoteViews;

public abstract class HSWidget extends android.appwidget.AppWidgetProvider {
	private static final String TAG = "HSWidget";

	private void addWidget(final int appWidgetId, final Context context,
			final AppWidgetManager appWidgetManager) {
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				getLayoutResId());
		getWidgetComponentNames().put(appWidgetId, getComponentName(context));
		getWidgetManagers().put(appWidgetId,
				AppWidgetManager.getInstance(context));
		getWidgetViews().put(appWidgetId, views);
		appWidgetManager.updateAppWidget(appWidgetId, views);
		onStart(appWidgetId, context);
	}

	protected abstract ComponentName getComponentName(Context context);

	protected abstract int getLayoutResId();

	protected abstract Map<Integer, ComponentName> getWidgetComponentNames();

	protected abstract Map<Integer, AppWidgetManager> getWidgetManagers();

	protected abstract Map<Integer, RemoteViews> getWidgetViews();

	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		Log.d(TAG, "onDeleted");
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			removeWidget(appWidgetIds[i]);
		}
	}

	@Override
	public void onDisabled(final Context context) {
		Log.d(TAG, "onDisabled");
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(getComponentName(context),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onEnabled(final Context context) {
		Log.d(TAG, "onEnabled");
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(getComponentName(context),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	protected abstract void onStart(int appWidgetId, Context context);

	protected abstract void onStop(int appWidgetId);

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		final int N = appWidgetIds.length;
		// Perform loop procedure for each Widget that belongs to this provider
		for (int i = 0; i < N; i++) {
			addWidget(appWidgetIds[i], context, appWidgetManager);
		}
	}

	private void removeWidget(final int appWidgetId) {
		onStop(appWidgetId);
		getWidgetComponentNames().remove(appWidgetId);
		getWidgetManagers().remove(appWidgetId);
		getWidgetViews().remove(appWidgetId);
	}
}
