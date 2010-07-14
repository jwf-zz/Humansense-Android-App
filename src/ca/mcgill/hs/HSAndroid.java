/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import ca.mcgill.hs.serv.HSService;

/**
 * This Activity is the entry point to the HSAndroid application. This Activity
 * is launched manually on the phone by the user, and is from where the
 * background services can be manually started and stopped, and where the
 * preferences and settigns can be changed.
 * 
 * @author Jonathan Pitre
 * 
 */
public class HSAndroid extends Activity {

	// TODO: Update strings.xml with new static strings.
	private class FileUploader {

		// The context used to access strings
		private final Context context;

		// The URL of the server to upload to.
		private final String URL_STRING = "http://www.cs.mcgill.ca/~ccojoc2/uploader.php";

		// Unuploaded dir path
		private final String UNUPLOADED_PATH;

		// The location of the files to upload.
		private final String DIR_PATH = (String) getResources().getText(
				R.string.data_file_path);

		private HttpURLConnection conn;

		// Format Strings for upload form.
		private final String lineEnd = "\r\n";
		private final String twoHyphens = "--";
		private final String boundary = "*****";

		// The files to upload to server.
		private final LinkedList<String> filesToUpload = new LinkedList<String>();

		// Upload finished intent.
		public static final String UPLOAD_COMPLETE_INTENT = "ca.mcgill.hs.HSAndroidApp.UPLOAD_COMPLETE_INTENT";

		// ERROR CODES
		private final int NO_ERROR_CODE = 0x0;
		private final int MALFORMEDURLEXCEPTION_ERROR_CODE = 0x1;
		private final int UNKNOWNHOSTEXCEPTION_ERROR_CODE = 0x2;
		private final int IOEXCEPTION_ERROR_CODE = 0x3;
		private final int UPLOAD_FAILED_ERROR_CODE = 0x4;
		private int ERROR_CODE = NO_ERROR_CODE;

		// Upload complete BroadcastReceiver
		private final BroadcastReceiver completionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				if (intent.getAction().equals(UPLOAD_COMPLETE_INTENT)) {
					onUploadComplete();
				}
			}
		};

		private FileUploader(final Context context) {
			this.context = context;

			UNUPLOADED_PATH = (String) context.getResources().getText(
					R.string.data_file_path)
					+ (String) context.getResources().getText(
							R.string.unuploaded_file_path);
		}

		private void addFiles() {
			final File path = new File(Environment
					.getExternalStorageDirectory(), UNUPLOADED_PATH);

			if (!path.isDirectory()) {
				if (!path.mkdirs()) {
					Log.e("Output Dir", "Could not create output directory!");
					return;
				}
			}

			final String[] files = path.list();
			for (final String s : files) {
				Log.e("Uploader", s);
			}

			if (files.length == 0) {
				return;
			} else {
				for (final String s : files) {
					filesToUpload.add(s);
				}
			}
		}

		/**
		 * Gets called whenever the file upload is complete.
		 */
		private void onUploadComplete() {
			switch (ERROR_CODE) {
			case NO_ERROR_CODE:
				makeToast(filesToUpload.size() + " files uploaded to server.",
						Toast.LENGTH_SHORT);
				break;
			case UNKNOWNHOSTEXCEPTION_ERROR_CODE:
				makeToast("Unable to connect to server.", Toast.LENGTH_SHORT);
				break;
			case UPLOAD_FAILED_ERROR_CODE:
				makeToast("One or more files have failed to upload.",
						Toast.LENGTH_SHORT);
			default:
				break;
			}
			uploading = false;
			uploadButton.setEnabled(true);
			uploadButton.setText("UPLOAD");
			unregisterReceiver(completionReceiver);
			filesToUpload.clear();
			conn.disconnect();
		}

		/**
		 * Uploads most recent files to the server defined in URL_STRING.
		 */
		private void upload() {
			addFiles();
			if (filesToUpload.size() == 0) {
				makeToast("No new files to upload.", Toast.LENGTH_SHORT);
				return;
			}
			uploading = true;
			uploadButton.setEnabled(false);
			uploadButton.setText("Uploading...");

			registerReceiver(completionReceiver, new IntentFilter(
					UPLOAD_COMPLETE_INTENT));

			// The thread in which the files will be uploaded.
			new Thread() {
				@Override
				public void run() {
					for (final String fileName : filesToUpload) {
						try {
							// Create connection from URL.
							final URL url = new URL(URL_STRING);

							conn = (HttpURLConnection) url.openConnection();

							conn.setDoOutput(true);

							conn.setRequestMethod("POST");

							conn.setRequestProperty("Connection", "Keep-Alive");

							conn.setRequestProperty("Content-Type",
									"multipart/form-data;boundary=" + boundary);

							final DataOutputStream dos = new DataOutputStream(
									conn.getOutputStream());

							final File fileToUpload = new File(
									Environment.getExternalStorageDirectory(),
									(String) context.getResources().getText(
											R.string.data_file_path)
											+ (String) context
													.getResources()
													.getText(
															R.string.unuploaded_file_path)
											+ "/" + fileName);

							final FileInputStream fis = new FileInputStream(
									fileToUpload);

							dos.writeBytes(twoHyphens + boundary + lineEnd);
							dos
									.writeBytes("Content-Disposition: post-data; name=uploadedfile;filename="
											+ fileName + "" + lineEnd);
							dos.writeBytes(lineEnd);

							int bytesAvailable = fis.available();

							final byte[] buffer = new byte[bytesAvailable];

							int bytesRead = fis.read(buffer, 0, bytesAvailable);

							while (bytesRead > 0) {
								dos.write(buffer, 0, bytesAvailable);
								bytesAvailable = fis.available();
								bytesRead = fis.read(buffer, 0, bytesAvailable);
							}

							dos.writeBytes(lineEnd);
							dos.writeBytes(twoHyphens + boundary + twoHyphens
									+ lineEnd);

							fis.close();
							dos.flush();
							dos.close();

							// Check server response
							final BufferedReader rd = new BufferedReader(
									new InputStreamReader(conn.getInputStream()));
							String line;
							while ((line = rd.readLine()) != null) {
								if (!line.endsWith("has been uploaded")) {
									ERROR_CODE = UPLOAD_FAILED_ERROR_CODE;
								}
								Log.e("HSAndroid Upload", "Message: " + line);
							}
							rd.close();

							final File dest = new File(
									Environment.getExternalStorageDirectory(),
									(String) context.getResources().getText(
											R.string.data_file_path)
											+ (String) context
													.getResources()
													.getText(
															R.string.uploaded_file_path));
							if (!dest.isDirectory()) {
								if (!dest.mkdirs()) {
									throw new IOException(
											"ERROR: Unable to create directory "
													+ dest.getName());
								}
							}

							if (!fileToUpload.renameTo(new File(dest,
									fileToUpload.getName()))) {
								throw new IOException(
										"ERROR: Unable to transfer file "
												+ fileToUpload.getName());
							}

						} catch (final MalformedURLException ex) {
							Log.e("HSAndroid Upload", "error: "
									+ ex.getMessage(), ex);
							ERROR_CODE = MALFORMEDURLEXCEPTION_ERROR_CODE;
						} catch (final UnknownHostException uhe) {
							Log.w("HSAndroid Upload", "Unable to connect...");
							ERROR_CODE = UNKNOWNHOSTEXCEPTION_ERROR_CODE;
						} catch (final IOException ioe) {
							Log.e("HSAndroid Upload", "error: "
									+ ioe.getMessage(), ioe);
							ERROR_CODE = IOEXCEPTION_ERROR_CODE;
						}

						conn.disconnect();
					}
					// When finished, broadcast a completion intent.
					final Intent i = new Intent();
					i.setAction(UPLOAD_COMPLETE_INTENT);
					sendBroadcast(i);
				}
			}.start();
		}
	}

	private static Button serviceSwitch;
	private static Button uploadButton;

	private Intent i;
	// Offsets used for all toasts
	private final int TOAST_Y_OFFSET = 100;

	private final int TOAST_X_OFFSET = 0;

	private boolean autoStartAppStart = false;
	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";

	private static final int MENU_SETTINGS = 13371337;
	private FileUploader fu;

	private static boolean uploading = false;

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
		autoStartAppStart = prefs.getBoolean("autoStartAtAppStart", false);
	}

	/**
	 * Helper method for making toasts.
	 * 
	 * @param message
	 *            the text to toast.
	 * @param duration
	 *            the duration of the toast.
	 */
	private void makeToast(final String message, final int duration) {
		final Toast slice = Toast.makeText(getBaseContext(), message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset()
				+ TOAST_X_OFFSET, slice.getYOffset() + TOAST_Y_OFFSET);
		slice.show();
	}

	/**
	 * This method is called when the activity is first created. It is the entry
	 * point for the application.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Creating the fu
		fu = new FileUploader(this);

		// Intent
		i = new Intent(this, HSService.class);

		// Setup preferences
		getPrefs();

		// Auto App Start
		if (autoStartAppStart) {
			startService(i);
		}

		// Buttons
		serviceSwitch = (Button) findViewById(R.id.button);
		serviceSwitch.setText(HSService.isRunning() ? R.string.stop_label
				: R.string.start_label);
		serviceSwitch.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				if (!HSService.isRunning()) { // NOT RUNNING
					startService(i);
					serviceSwitch.setText(R.string.stop_label);
				} else { // RUNNING
					stopService(i);
					serviceSwitch.setText(R.string.start_label);
				}
			}
		});

		// YES-NO DIALOG BOX FOR FILE UPLOAD
		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					fu.upload();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Upload data collected to online server?")
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener);

		uploadButton = (Button) findViewById(R.id.uploadButton);
		uploadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				builder.show();
			}
		});

		if (uploading) {
			uploadButton.setEnabled(false);
			uploadButton.setText("Uploading...");
		}

	}

	/**
	 * This method is called whenever the user wants to access the settings
	 * menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(0, MENU_SETTINGS, 0, R.string.settingString).setIcon(
				R.drawable.options);
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
		}
		return false;
	}

}