/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ca.mcgill.hs.prefs.HSAndroidPreferences;
import ca.mcgill.hs.serv.HSService;

/**
 * This Activity is the entry point to the HSAndroid application. This Activity
 * is launched manually on the phone by the user, and is from where the
 * background services can be manually started and stopped, and where the
 * preferences and settings can be changed.
 * 
 * @author Jonathan Pitre
 * 
 */
public class HSAndroid extends Activity {

	private static Button serviceSwitch;

	private Intent serviceIntent;

	private boolean autoStartAppStart = false;
	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";

	private static final int MENU_SETTINGS = 13371337;
	private static final int MENU_UPLOAD = 13371338;

	private static final String TAG = "HSAndroid";

	/**
	 * Updates the main starting button. This is required due to the nature of
	 * Activities in the Android API. In order to correctly get the state of the
	 * service to update the button text, this method cannot be called from
	 * within the Activity.
	 */
	public static void updateButton() {
		if (serviceSwitch != null) {
			serviceSwitch.setText((HSService.isRunning() ? R.string.stop_label
					: R.string.start_label));
		}
	}

	/**
	 * Sets up the preferences, i.e. get Activity preferences.
	 */
	private void getPrefs() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		autoStartAppStart = prefs.getBoolean(
				HSAndroidPreferences.AUTO_START_AT_APP_START_PREF, false);
	}

	/**
	 * This method is called when the activity is first created. It is the entry
	 * point for the application.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// // final Sensor s = new Sensor();
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

		setContentView(R.layout.main);

		// Intent
		serviceIntent = new Intent(this, HSService.class);

		// Setup preferences
		getPrefs();

		// Auto App Start
		if (autoStartAppStart) {
			startService(serviceIntent);
		}

		// Buttons
		serviceSwitch = (Button) findViewById(R.id.button);
		serviceSwitch.setText(HSService.isRunning() ? R.string.stop_label
				: R.string.start_label);
		serviceSwitch.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				if (!HSService.isRunning()) { // NOT RUNNING
					Debug.startMethodTracing("hsandroid");
					startService(serviceIntent);
					serviceSwitch.setText(R.string.stop_label);
				} else { // RUNNING
					stopService(serviceIntent);
					serviceSwitch.setText(R.string.start_label);
					Debug.stopMethodTracing();
				}
			}
		});

		// Intent for the Uploader service
		/*
		 * final Intent uploaderIntent = new Intent(this,
		 * UploaderService.class);
		 * 
		 * // YES-NO DIALOG BOX FOR FILE UPLOAD final
		 * DialogInterface.OnClickListener dialogClickListener = new
		 * DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick(final DialogInterface dialog, final int
		 * which) { switch (which) { case DialogInterface.BUTTON_POSITIVE:
		 * startService(uploaderIntent); break;
		 * 
		 * case DialogInterface.BUTTON_NEGATIVE: break; } } };
		 * 
		 * final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 * builder.setMessage(R.string.upload_query).setPositiveButton(
		 * R.string.yes, dialogClickListener).setNegativeButton( R.string.no,
		 * dialogClickListener);
		 */

	}

	/**
	 * This method is called whenever the user wants to access the settings
	 * menu.
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
	 * This method is used to parse the selection of options items. These items
	 * include: - Preferences (settings)
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
					ca.mcgill.hs.serv.NewUploaderService.class);
			startService(uploaderIntent);
			break;
		}
		return false;
	}

}