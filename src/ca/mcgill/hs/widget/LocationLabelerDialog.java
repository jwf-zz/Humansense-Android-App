package ca.mcgill.hs.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import ca.mcgill.hs.R;

public class LocationLabelerDialog extends android.app.Activity {
	public static final String TAG = "LocationLabelerDialog";
	private int appWidgetId;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		final Context context = getApplicationContext();
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.location_status_appwidget);
		appWidgetManager.updateAppWidget(appWidgetId, views);
		final Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		super.onDestroy();
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();

	}

	@Override
	public void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		/*
		 * Store the widget id that launched this activity, used to update the
		 * widget when we're done with this activity.
		 */
		final Intent intent = getIntent();
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		setContentView(R.layout.location_labeler_dialog);
		final TextView text = (TextView) findViewById(R.id.location_labeler_top_text);
		text.setText("Last location has id: "
				+ LocationStatusWidget.getLastLocationId());
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
}
