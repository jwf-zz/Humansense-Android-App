/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import ca.mcgill.hs.plugin.PluginFactory;
import ca.mcgill.hs.prefs.HSAndroidPreferences;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.serv.HSService;
import ca.mcgill.hs.util.Log;

/**
 * This Activity is the entry point to the HSAndroid application. This Activity
 * is launched manually on the phone by the user, and is from where the
 * background services can be manually started and stopped, and where the
 * preferences and settings can be changed.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class HSAndroid extends Activity {

	private static Button serviceSwitch;

	private Intent serviceIntent;

	/**
	 * Whether to start the service automatically when the application is
	 * loaded.
	 */
	private boolean autoStartAppStart = false;

	/**
	 * Whether to use Debug.MethodTracing to collect profiling information. Make
	 * sure that this is false for a release version!
	 */
	private static final boolean doProfiling = false;

	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";
	private static Context context = null;
	private static TableLayout freeSpace = null;

	private static final int MENU_SETTINGS = 13371337;
	private static final int MENU_UPLOAD = 13371338;

	private static final String TAG = "HSAndroid";

	/**
	 * Fetches a resource string for the specified id from the application
	 * context.
	 * 
	 * @param resourceId
	 *            for the string to be fetched.
	 * @return String corresponding to the resourceId
	 */
	public static String getAppString(final int resourceId) {
		return context.getString(resourceId);
	}

	/**
	 * Returns a handle to the free space in the main screen where plugins can
	 * add messages or widgets.
	 * 
	 * @return Handle to the free space on the main application layout.
	 */
	public static TableLayout getFreeSpace() {
		return freeSpace;
	}

	/**
	 * Updates the main starting button according to whether the service is
	 * running or not. Should be called whenever the state of the service is
	 * changed. Probably a way to do this using Intents or events, but for now
	 * we rely on it being called manually.
	 */
	public static void updateButton() {
		if (serviceSwitch != null) {
			if (HSService.isRunning()) {
				serviceSwitch.setText(R.string.stop_label);
			} else {
				serviceSwitch.setText(R.string.start_label);
			}
		}
	}

	/**
	 * Retrieves the current state of the main application preferences, which
	 * control whether log data is stored in a file and whether the service
	 * automatically starts when the application is loaded.
	 */
	private void getPrefs() {
		final SharedPreferences prefs = PreferenceFactory
				.getSharedPreferences(this);
		autoStartAppStart = prefs.getBoolean(
				HSAndroidPreferences.AUTO_START_AT_APP_START_PREF, false);
		final boolean logToFile = prefs.getBoolean(
				HSAndroidPreferences.LOG_TO_FILE_PREF, false);
		Log.setLogToFile(logToFile);
	}

	/**
	 * This method is called when the activity is first created. It is the entry
	 * point for the application. Here we set up some important member variables
	 * such as the application context, the plugin factory, the input and output
	 * plugins, and the application preferences. We also initialize the GUI
	 * here.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = getApplicationContext();
		PreferenceFactory.setContext(context);

		// Setup preferences
		getPrefs();

		PluginFactory.setContext(context);
		HSService.initializeInputPlugins();
		HSService.initializeOutputPlugins();

		// Testing code for new sensor interface, disabled for now:
		// final Sensor s = new Sensor();
		// Log.d(TAG, "Sensor.androidInit: " + Sensor.androidInit());
		// Log.d(TAG, "Bundle: " + Sensor.androidOpen());
		// Log.d(TAG, "Sensor.sensorsModuleInit: " +
		// Sensor.sensorsModuleInit());
		// Log.d(TAG, "Sensor.sensorsDataInit: " + Sensor.sensorsDataInit());
		//
		// final Sensor sensor = new Sensor();
		// Log.d(TAG, "Sensor.sensorsModuleGetNextSensor: "
		// + Sensor.sensorsModuleGetNextSensor(sensor, 0));
		// Log.d(TAG, "Sensor Name: " + sensor.getName());
		// final float[] values = new float[3];
		// final int[] accuracy = new int[1];
		// final long[] timestamp = new long[1];
		// Log.d(TAG, "Sensor.sensorsDataPoll: "
		// + Sensor.sensorsDataPoll(values, accuracy, timestamp));
		// Log.d(TAG, "\tSensor output: " + values[0] + ", " + values[1] + ", "
		// + values[2]);
		//
		// Log.d(TAG, "Sensor.sensorsDataUnInit: " +
		// Sensor.sensorsDataUninit());
		// s.sensors_module_get_next_sensor(new Object(), 1);
		// Sensor.sensors_module_init();

		/* Set up the GUI */
		setContentView(R.layout.main);
		freeSpace = (TableLayout) findViewById(R.id.free_space);

		// Intent
		serviceIntent = new Intent(this, HSService.class);

		// Buttons
		serviceSwitch = (Button) findViewById(R.id.button);
		serviceSwitch.setText(R.string.start_label);
		serviceSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (!HSService.isRunning()) { // NOT RUNNING
					if (doProfiling) {
						Debug.startMethodTracing(TAG);
					}
					startService(serviceIntent);
				} else { // RUNNING
					stopService(serviceIntent);
					if (doProfiling) {
						Debug.stopMethodTracing();
					}
				}
				updateButton();
			}
		});

		if (autoStartAppStart) {
			// Start the service on application start.
			startService(serviceIntent);
		}
	}

	/**
	 * Called when the user access the application's options menu, sets up the
	 * two icons that appear.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(0, MENU_SETTINGS, 0, R.string.settingString).setIcon(
				R.drawable.options);
		menu.add(0, MENU_UPLOAD, 1, R.string.uploadString).setIcon(
				R.drawable.upload);
		return true;
	}

	/**
	 * Loads the appropriate preferences screen depending on which option was
	 * selected from the options menu.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			final Intent i = new Intent(getBaseContext(),
					ca.mcgill.hs.prefs.HSAndroidPreferences.class);
			startActivity(i);
			break;
		case MENU_UPLOAD:
			final Intent uploaderIntent = new Intent(getBaseContext(),
					ca.mcgill.hs.serv.LogFileUploaderService.class);
			startService(uploaderIntent);
			break;
		}
		return false;
	}

}