package ca.mcgill.hs.serv;

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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;

public class UploaderService extends Service {

	// The intent of this service
	private Intent shutdownIntent;

	// The URL of the server to upload to.
	private final String URL_STRING = "http://www.cs.mcgill.ca/~ccojoc2/uploader.php";

	// Unuploaded dir path
	private String UNUPLOADED_PATH;

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
				stopService(shutdownIntent);
			}
		}
	};

	/**
	 * Goes through the recent folder and adds all the files to the list of
	 * files to upload.
	 */
	private void addFiles() {
		final File path = new File(Environment.getExternalStorageDirectory(),
				UNUPLOADED_PATH);

		if (!path.isDirectory()) {
			if (!path.mkdirs()) {
				Log.e("Output Dir", "Could not create output directory!");
				return;
			}
		}

		final String[] files = path.list();

		if (files.length == 0) {
			return;
		} else {
			for (final String s : files) {
				filesToUpload.add(UNUPLOADED_PATH + s);
			}
		}
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
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	@Override
	public IBinder onBind(final Intent arg0) {
		return null;
	}

	/**
	 * Called when the service is started. Creates the service.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		UNUPLOADED_PATH = (String) getBaseContext().getResources().getText(
				R.string.recent_file_path);

		shutdownIntent = new Intent(this, UploaderService.class);
	}

	/**
	 * Called when the service is stopped.
	 */
	@Override
	public void onDestroy() {
		onUploadComplete();
	}

	/**
	 * Called automatically when onCreate() is called. Contains the code for
	 * uploading files to the server.
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		addFiles();
		if (filesToUpload.size() == 0) {
			makeToast("No new files to upload.", Toast.LENGTH_SHORT);
			return;
		}
		HSAndroid.uploading = true;
		HSAndroid.uploadButton.setEnabled(false);
		HSAndroid.uploadButton.setText("Uploading...");

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

						// Make appropriate property requests
						conn.setDoOutput(true);

						conn.setRequestMethod("POST");

						conn.setRequestProperty("Connection", "Keep-Alive");

						conn.setRequestProperty("Content-Type",
								"multipart/form-data;boundary=" + boundary);

						final DataOutputStream dos = new DataOutputStream(conn
								.getOutputStream());

						final File fileToUpload = new File(Environment
								.getExternalStorageDirectory(), fileName);

						final FileInputStream fis = new FileInputStream(
								fileToUpload);
						/*
						 * dos.writeBytes(twoHyphens + boundary + lineEnd); dos
						 * .writeBytes(
						 * "Content-Disposition: post-data; type=\"text\";name=\"phoneid\";value=\"fail\""
						 * + lineEnd); dos.writeBytes(lineEnd);
						 */
						// Format files into form format
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
						}
						rd.close();

						// Move files to uploaded folder
						final File dest = new File(Environment
								.getExternalStorageDirectory(),
								(String) getResources().getText(
										R.string.uploaded_file_path));
						if (!dest.isDirectory()) {
							if (!dest.mkdirs()) {
								throw new IOException(
										"ERROR: Unable to create directory "
												+ dest.getName());
							}
						}

						if (!fileToUpload.renameTo(new File(dest, fileToUpload
								.getName()))) {
							throw new IOException(
									"ERROR: Unable to transfer file "
											+ fileToUpload.getName());
						}

					} catch (final MalformedURLException ex) {
						Log.e("HSAndroid Upload", "error: " + ex.getMessage(),
								ex);
						ERROR_CODE = MALFORMEDURLEXCEPTION_ERROR_CODE;
					} catch (final UnknownHostException uhe) {
						Log.w("HSAndroid Upload", "Unable to connect...");
						ERROR_CODE = UNKNOWNHOSTEXCEPTION_ERROR_CODE;
					} catch (final IOException ioe) {
						Log.e("HSAndroid Upload", "error: " + ioe.getMessage(),
								ioe);
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

	/**
	 * Gets called whenever the file upload is complete. Checks for errors and
	 * resets the Upload button.
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
		HSAndroid.uploading = false;
		HSAndroid.uploadButton.setEnabled(true);
		HSAndroid.uploadButton.setText("UPLOAD");
		unregisterReceiver(completionReceiver);
		filesToUpload.clear();
		conn.disconnect();
	}

}
