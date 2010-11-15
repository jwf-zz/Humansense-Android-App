/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import ca.mcgill.hs.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.LocationClusterer;

/**
 * The dialog for labeling locations. This is called by the LocationStatusWidget
 * when someone
 */
public class LocationLabelerDialog extends Activity {
	public static final String TAG = "LocationLabelerDialog";
	private int appWidgetId;

	private String getClusterName(final long id) {
		final LocationClusterer.LocationDictionaryOpenHelper dbOpener = new LocationClusterer.LocationDictionaryOpenHelper(
				this.getApplicationContext());
		final SQLiteDatabase locationDatabase = dbOpener.getReadableDatabase();
		String clusterName;
		final Cursor cursor = locationDatabase.query("locations",
				new String[] { "location_name" }, "id=?", new String[] { Long
						.toString(id) }, null, null, null, "1");
		if (cursor.moveToFirst()) {
			clusterName = cursor.getString(0);
		} else {
			clusterName = null;
		}
		cursor.close();
		dbOpener.close();
		return clusterName;
	}

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
	public Object onRetainNonConfigurationInstance() {
		// final MyDataObject data = collectMyLoadedData();
		return null;
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
		final long clusterID;
		String clusterName;
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		clusterID = LocationStatusWidget.getMostRecentClusterId();
		clusterName = getClusterName(clusterID);
		setContentView(R.layout.location_labeler_dialog);
		final TextView text = (TextView) findViewById(R.id.location_labeler_top_text);
		text.setText("Please enter a location label for location " + clusterID);
		final Button okButton = (Button) findViewById(R.id.location_input_ok_button);
		final Button cancelButton = (Button) findViewById(R.id.location_input_cancel_button);
		final EditText textInput = (EditText) findViewById(R.id.location_input_edittext);
		if (clusterName != null) {
			textInput.setText(clusterName);
		}
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final LocationClusterer.LocationDictionaryOpenHelper dbOpener = new LocationClusterer.LocationDictionaryOpenHelper(
						v.getContext());
				final SQLiteDatabase locationDatabase = dbOpener
						.getWritableDatabase();
				final ContentValues values = new ContentValues();
				values.put("id", clusterID);
				values.put("location_name", textInput.getText().toString());
				Log.d(TAG, "Adding Location named " + textInput.getText()
						+ " with id " + clusterID);
				locationDatabase.insertWithOnConflict("locations", null,
						values, SQLiteDatabase.CONFLICT_REPLACE);
				locationDatabase.close();
				dbOpener.close();
				finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
}
