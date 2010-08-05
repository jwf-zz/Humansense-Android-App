package ca.mcgill.hs.serv;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import ca.mcgill.hs.HSAndroid;
import ca.mcgill.hs.R;

public class UploaderService extends Service {

	// The intent of this service
	private Intent shutdownIntent;

	// Unuploaded dir path
	private String UNUPLOADED_PATH;

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
			makeToast(getResources().getString(R.string.uploader_no_new_files),
					Toast.LENGTH_SHORT);
			return;
		}
		HSAndroid.uploading = true;
		HSAndroid.uploadButton.setEnabled(false);
		HSAndroid.uploadButton.setText(R.string.uploading_label);

		registerReceiver(completionReceiver, new IntentFilter(
				UPLOAD_COMPLETE_INTENT));

		final WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		final WifiInfo wi = wm.getConnectionInfo();

		// The thread in which the files will be uploaded.
		new Thread() {
			@Override
			public void run() {
				for (final String fileName : filesToUpload) {
					final HttpClient httpclient = new DefaultHttpClient();
					httpclient.getParams().setParameter(
							CoreProtocolPNames.PROTOCOL_VERSION,
							HttpVersion.HTTP_1_0);

					final HttpPost httppost = new HttpPost(
							"http://cgi.cs.mcgill.ca/~ccojoc2/uploader.php");
					final File file = new File(Environment
							.getExternalStorageDirectory(), fileName);
					httppost.addHeader("MAC", wi.getMacAddress());
					final MultipartEntity mpEntity = new MultipartEntity();
					final ContentBody cbFile = new FileBody(file,
							"binary/octet-stream");
					mpEntity.addPart("uploadedfile", cbFile);

					httppost.setEntity(mpEntity);
					try {
						final HttpResponse response = httpclient
								.execute(httppost);
						final HttpEntity resEntity = response.getEntity();

						String responseMsg = "";
						if (resEntity != null) {
							responseMsg = EntityUtils.toString(resEntity);
							Log.i("Server Response", responseMsg);
						}
						if (resEntity != null) {
							resEntity.consumeContent();
						}

						if (!responseMsg.contains("SUCCESS 0x64asv65")) {
							ERROR_CODE = UPLOAD_FAILED_ERROR_CODE;
						}
						// Move files to uploaded folder if successful
						if (ERROR_CODE == NO_ERROR_CODE) {
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

							if (!file.renameTo(new File(dest, file.getName()))) {
								throw new IOException(
										"ERROR: Unable to transfer file "
												+ file.getName());
							}
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

					httpclient.getConnectionManager().shutdown();
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
			final int fileCount = filesToUpload.size();
			if (fileCount == 1) {
				makeToast(getResources().getString(
						R.string.uploader_no_errors_one_file),
						Toast.LENGTH_SHORT);
			} else {
				makeToast(fileCount
						+ " "
						+ getResources().getString(
								R.string.uploader_no_errors_multiple_files),
						Toast.LENGTH_SHORT);
			}
			break;
		case UNKNOWNHOSTEXCEPTION_ERROR_CODE:
			makeToast(getResources().getString(
					R.string.uploader_unable_to_connect), Toast.LENGTH_SHORT);
			break;
		case UPLOAD_FAILED_ERROR_CODE:
			makeToast(
					getResources().getString(R.string.uploader_upload_failed),
					Toast.LENGTH_SHORT);
		default:
			break;
		}
		HSAndroid.uploading = false;
		HSAndroid.uploadButton.setEnabled(true);
		HSAndroid.uploadButton.setText(R.string.upload_label);
		unregisterReceiver(completionReceiver);
		filesToUpload.clear();
	}

}
