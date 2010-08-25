package ca.mcgill.hs.serv;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import ca.mcgill.hs.R;

/**
 * A new uploader service for the HSAndroid Application. When this service
 * starts, it retrieves the list of saved files. Then, if an option is set, it
 * will attempt to connect 3 times to a wifi connection. Afterwards, it will
 * upload the files, recording error codes. Finally, it will toast the user to
 * alert them of the success or failure of the operation.
 * 
 * If the service was already started, then it will simply update the list of
 * files and resume its operations.
 * 
 * @authors Cicerone Cojocaru, Jonathan Pitre
 */
public class NewUploaderService extends Service {

	/* STATE BOOLEANS */
	// This boolean indicates whether or not this service was started or not.
	private static boolean started = false;

	private static final String TAG = "HSUploaderService";

	/* LIST OF FILES */
	private static final LinkedList<String> fileList = new LinkedList<String>();
	private static final HashMap<String, Object> fileMap = new HashMap<String, Object>();

	/* ERROR CODES */
	public static final int NO_ERROR_CODE = 0x0;
	public static final int MALFORMEDURLEXCEPTION_ERROR_CODE = 0x1;
	public static final int UNKNOWNHOSTEXCEPTION_ERROR_CODE = 0x2;
	public static final int IOEXCEPTION_ERROR_CODE = 0x3;
	public static final int UPLOAD_FAILED_ERROR_CODE = 0x4;
	private int FINAL_ERROR_CODE;
	private int TEMP_ERROR_CODE;

	private WifiManager wm;
	private WifiInfo wi;

	private int filesUploaded;

	/* CONNECTION VARIABLES */
	private int connection_attempts;
	private final boolean connection_successful = false;
	private HttpClient httpclient;
	private HttpPost httppost;
	private boolean wifiOnly;
	private boolean automatic;
	private ConnectivityManager cm;

	/* NOTIFICATION VARIABLES */
	public final int NOTIFICATION_ID = 455926;
	private final String NOTIFICATION_STRING = Context.NOTIFICATION_SERVICE;
	private NotificationManager nm;

	/* UPLOAD COMPLETION INTENTS */
	public static final String UPLOAD_COMPLETE_INTENT = "ca.mcgill.hs.HSAndroidApp.UPLOAD_COMPLETE_INTENT";
	private Intent shutdownIntent;
	private final BroadcastReceiver completionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (intent.getAction().equals(UPLOAD_COMPLETE_INTENT)) {
				stopService(shutdownIntent);
			}
		}
	};

	/* FILE VARIABLES */
	private String UNUPLOADED_PATH;

	private boolean canUpload() {
		if (!wifiOnly) {
			if (cm.getActiveNetworkInfo().getState() != NetworkInfo.State.CONNECTED) {
				return false;
			}
		} else {
			if (cm.getActiveNetworkInfo().getState() != NetworkInfo.State.CONNECTED
					|| cm.getActiveNetworkInfo().getType() != ConnectivityManager.TYPE_WIFI) {
				return false;
			}
		}
		return true;
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
		final Toast slice = Toast.makeText(getBaseContext(), getResources()
				.getString(R.string.uploader_appname_label)
				+ message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	@Override
	public IBinder onBind(final Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		FINAL_ERROR_CODE = NO_ERROR_CODE;
		shutdownIntent = new Intent(this, NewUploaderService.class);
		UNUPLOADED_PATH = (String) getBaseContext().getResources().getText(
				R.string.recent_file_path);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		started = false;
		onUploadComplete();
	}

	@Override
	public synchronized void onStart(final Intent intent, final int startId) {
		// Update the file list
		updateFileList();

		// If there are no files, return.
		if (fileList.size() == 0 && !started) {
			makeToast(getResources().getString(R.string.uploader_no_new_files),
					Toast.LENGTH_SHORT);
			return;
		}

		// If it was already started, return. Else, continue.
		if (started) {
			return;
		}

		// At this point we consider the service to be started.
		started = true;

		wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wi = wm.getConnectionInfo();

		// Register completion receiver
		registerReceiver(completionReceiver, new IntentFilter(
				UPLOAD_COMPLETE_INTENT));

		// Connect to a network
		setUpConnection();

		nm = (NotificationManager) getSystemService(NOTIFICATION_STRING);
		nm.cancel(NOTIFICATION_ID);

		final int icon = R.drawable.notification_icon;
		final String tickerText = getResources().getString(
				R.string.notification_ticker);
		final String contentTitle = getResources().getString(
				R.string.notification_upload_title);
		final String contentText = getResources().getString(
				R.string.notification_upload_text);

		final Notification n = new Notification(icon, tickerText, System
				.currentTimeMillis());

		final Intent i = new Intent(this, HSService.class);
		n.setLatestEventInfo(this, contentTitle, contentText, PendingIntent
				.getActivity(this.getBaseContext(), 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT));

		nm.notify(NOTIFICATION_ID, n);

		filesUploaded = 0;

		new Thread() {
			@Override
			public void run() {
				uploadFiles();
			}
		}.start();
	}

	/**
	 * Gets called whenever the file upload is complete.
	 */
	private void onUploadComplete() {
		nm.cancel(NOTIFICATION_ID);

		switch (FINAL_ERROR_CODE) {
		case NO_ERROR_CODE:
			if (filesUploaded == 1) {
				makeToast(getResources().getString(
						R.string.uploader_no_errors_one_file),
						Toast.LENGTH_SHORT);
			} else {
				makeToast(filesUploaded
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
		unregisterReceiver(completionReceiver);
	}

	/**
	 * Attempts 3 times to connect to a network.
	 */
	private void setUpConnection() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		wifiOnly = prefs.getBoolean("uploadWifiOnly", false);
		automatic = prefs.getBoolean("autoUploadData", false);

		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * Goes through the recent folder and adds all the files to the list of
	 * files to upload.
	 */
	private synchronized void updateFileList() {

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
				if (!fileMap.containsKey(s)) {
					fileMap.put(s, null);
					fileList.add(UNUPLOADED_PATH + s);
				}
			}
		}
	}

	private boolean uploadFile(final String fileName) {
		TEMP_ERROR_CODE = NO_ERROR_CODE;
		Log.d(TAG, "Uploading " + fileName);
		httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(
				CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);

		httppost = new HttpPost("http://www.cs.mcgill.ca/~ccojoc2/uploader.php");
		final File file = new File(Environment.getExternalStorageDirectory(),
				fileName);
		httppost.addHeader("MAC", wi.getMacAddress());
		final MultipartEntity mpEntity = new MultipartEntity();
		final ContentBody cbFile = new FileBody(file, "binary/octet-stream");
		mpEntity.addPart("uploadedfile", cbFile);

		httppost.setEntity(mpEntity);
		try {
			final HttpResponse response = httpclient.execute(httppost);
			final HttpEntity resEntity = response.getEntity();

			String responseMsg = "";
			if (resEntity != null) {
				responseMsg = EntityUtils.toString(resEntity);
				Log.i(TAG, "Server Response: " + responseMsg);
			}
			if (resEntity != null) {
				resEntity.consumeContent();
			}

			if (!responseMsg.contains("SUCCESS 0x64asv65")) {
				Log.i(TAG, "Server Response: " + responseMsg);
				FINAL_ERROR_CODE = UPLOAD_FAILED_ERROR_CODE;
				TEMP_ERROR_CODE = UPLOAD_FAILED_ERROR_CODE;
			}
			// Move files to uploaded folder if successful
			if (TEMP_ERROR_CODE == NO_ERROR_CODE) {
				final File dest = new File(Environment
						.getExternalStorageDirectory(), (String) getResources()
						.getText(R.string.uploaded_file_path));
				if (!dest.isDirectory()) {
					if (!dest.mkdirs()) {
						throw new IOException(
								"ERROR: Unable to create directory "
										+ dest.getName());
					}
				}

				if (!file.renameTo(new File(dest, file.getName()))) {
					throw new IOException("ERROR: Unable to transfer file "
							+ file.getName());
				}
			}

		} catch (final MalformedURLException ex) {
			Log.e(TAG, "error: " + ex.getMessage(), ex);
			FINAL_ERROR_CODE = MALFORMEDURLEXCEPTION_ERROR_CODE;
			return false;
		} catch (final UnknownHostException uhe) {
			Log.w(TAG, "Unable to connect...");
			FINAL_ERROR_CODE = UNKNOWNHOSTEXCEPTION_ERROR_CODE;
			return false;
		} catch (final IOException ioe) {
			Log.e(TAG, "error: " + ioe.getMessage(), ioe);
			FINAL_ERROR_CODE = IOEXCEPTION_ERROR_CODE;
			return false;
		}
		return true;
	}

	private void uploadFiles() {
		while (!fileList.isEmpty()) {
			if (canUpload()) {
				final String f = fileList.remove();
				final boolean success = uploadFile(f);
				if (!success) {
					fileList.addFirst(f);
				}
				filesUploaded++;
			} else {
				makeToast(getResources().getString(
						R.string.uploader_no_connection), Toast.LENGTH_SHORT);
				break;
			}
		}
		fileMap.clear();
		fileList.clear();
		httpclient.getConnectionManager().shutdown();
		final Intent i = new Intent();
		i.setAction(UPLOAD_COMPLETE_INTENT);
		sendBroadcast(i);
	}

}
