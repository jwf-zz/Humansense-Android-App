package ca.mcgill.hs.serv;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

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
import android.util.Log;
import android.widget.Toast;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.HSAndroidPreferences;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * A new uploader service for the HSAndroid Application. When this service
 * starts, it retrieves the list of saved files. Then, if an option is set, it
 * will attempt to connect 3 times to a wifi connection. Afterwards, it will
 * upload the files, recording error codes. Finally, it will toast the user to
 * alert them of the success or failure of the operation.
 * 
 * If the service was already started, then it will simply update the list of
 * files and resume its operations.
 */
public class LogFileUploaderService extends Service {
	private static final String TAG = "HSUploaderService";

	// Intent for when the auto-upload option is changed.
	public static final String AUTO_UPLOAD_CHANGED_INTENT = "ca.mcgill.hs.HSAndroidApp.AUTO_UPLOAD_CHANGED_INTENT";

	// Intent for when the wifi-only option is changed.
	public static final String WIFI_ONLY_CHANGED_INTENT = "ca.mcgill.hs.HSAndroidApp.WIFI_ONLY_CHANGED_INTENT";

	// Intent for when a new file is ready to be uploaded.
	public static final String FILE_ADDED_INTENT = "ca.mcgill.hs.HSAndroidApp.FILE_ADDED_INTENT";

	/**
	 * URL for uploading data files.
	 */
	public static final String UPLOAD_URL = "http://www.cs.mcgill.ca/~jfrank8/humansense/uploader.php";

	private boolean connectionReceiverRegistered = false;
	private boolean waiting = false;
	private Timer timer = new Timer();
	private final long DELAY = 30000;

	private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (((NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO))
					.isConnected()) {
				networkChanged();
			}
		}
	};

	// BroadcastReceiver for Use Wifi only preference change
	private final BroadcastReceiver wifiOnlyPrefChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			wifiPrefChanged();
		}
	};

	// Broadcast receiver for automatic upload preference changed
	private final BroadcastReceiver autoPrefChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			autoPrefChanged();
		}
	};

	// Keep track of whether the service has been started or not.
	private static boolean started = false;

	/* LIST OF FILES */
	private static final LinkedList<String> fileList = new LinkedList<String>();
	private static final HashMap<String, Object> fileMap = new HashMap<String, Object>();

	/* ERROR CODES */
	public static final int NO_ERROR_CODE = 0x0;
	public static final int MALFORMEDURLEXCEPTION_ERROR_CODE = 0x1;
	public static final int UNKNOWNHOSTEXCEPTION_ERROR_CODE = 0x2;
	public static final int IOEXCEPTION_ERROR_CODE = 0x3;
	public static final int UPLOAD_FAILED_ERROR_CODE = 0x4;
	public static final int NO_CONNECTION_ERROR = 0x5;
	private int FINAL_ERROR_CODE;
	private int TEMP_ERROR_CODE;

	/* WIFI MANAGER */
	private WifiManager wifiMgr;
	private WifiInfo wifiInfo;

	// Count of files uploaded
	private int filesUploaded;

	/* CONNECTION VARIABLES */
	private HttpClient httpclient;
	private HttpPost httppost;
	private boolean wifiOnly;
	private boolean automatic;
	private ConnectivityManager connectivityMgr;

	/* NOTIFICATION VARIABLES */
	private static final String NOTIFICATION_STRING = Context.NOTIFICATION_SERVICE;
	public static final int NOTIFICATION_ID = NOTIFICATION_STRING.hashCode();
	private NotificationManager notificationMgr;

	/* UPLOAD COMPLETION INTENTS */
	public static final String UPLOAD_COMPLETE_INTENT = "ca.mcgill.hs.HSAndroidApp.UPLOAD_COMPLETE_INTENT";
	private static Intent shutdownIntent;

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

	private static SharedPreferences prefs;

	/**
	 * Called when the auto upload preference has changed.
	 */
	private void autoPrefChanged() {
		automatic = prefs.getBoolean(
				HSAndroidPreferences.AUTO_UPLOAD_DATA_PREF, false);

		/*
		 * If auto uploading has just been turned off and we were waiting, kill
		 * the timer and stop the service.
		 */
		if (!automatic) {
			if (waiting) {
				timerKill();
				stopService(shutdownIntent);
			}
		}
	}

	/**
	 * Determines whether or not the phone is able to upload data over the
	 * internet.
	 * 
	 * @return True if it is possible to upload, false otherwise.
	 */
	private boolean canUpload() {
		if (connectivityMgr == null
				|| connectivityMgr.getActiveNetworkInfo() == null) {
			return false;
		}
		if (!wifiOnly) {
			if (connectivityMgr.getActiveNetworkInfo().getState() != NetworkInfo.State.CONNECTED) {
				return false;
			}
		} else {
			if (connectivityMgr.getActiveNetworkInfo().getState() != NetworkInfo.State.CONNECTED
					|| connectivityMgr.getActiveNetworkInfo().getType() != ConnectivityManager.TYPE_WIFI) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Helper method for making toasts.
	 * 
	 * @param message
	 *            The text to toast.
	 * @param duration
	 *            The duration of the toast.
	 */
	private void makeToast(final String message, final int duration) {
		final Toast slice = Toast.makeText(getBaseContext(), getResources()
				.getString(R.string.uploader_appname_label)
				+ message, duration);
		slice.setGravity(slice.getGravity(), slice.getXOffset(), slice
				.getYOffset() + 100);
		slice.show();
	}

	/**
	 * Called when there has been a change in network connectivity.
	 */
	private void networkChanged() {
		unregisterConnectReceiver();
		uploadFiles();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceFactory.getSharedPreferences();
		FINAL_ERROR_CODE = NO_ERROR_CODE;
		shutdownIntent = new Intent(this, LogFileUploaderService.class);
		UNUPLOADED_PATH = (String) getBaseContext().getResources().getText(
				R.string.recent_file_path);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		waiting = false;
		started = false;
		timer.cancel();
		unregisterConnectReceiver();
		unregisterReceiver(wifiOnlyPrefChanged);
		unregisterReceiver(autoPrefChanged);
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

		registerReceiver(wifiOnlyPrefChanged, new IntentFilter(
				WIFI_ONLY_CHANGED_INTENT));
		registerReceiver(autoPrefChanged, new IntentFilter(
				AUTO_UPLOAD_CHANGED_INTENT));

		wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiInfo = wifiMgr.getConnectionInfo();

		// Register completion receiver
		registerReceiver(completionReceiver, new IntentFilter(
				UPLOAD_COMPLETE_INTENT));

		// Connect to a network
		setUpConnection();

		notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_STRING);
		notificationMgr.cancel(NOTIFICATION_ID);

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

		notificationMgr.notify(NOTIFICATION_ID, n);

		filesUploaded = 0;
		uploadFiles();
	}

	/**
	 * Called whenever the file upload is complete. Notifies user if upload was
	 * manual.
	 */
	private void onUploadComplete() {
		notificationMgr.cancel(NOTIFICATION_ID);
		// If we are on automatic uploading, do not notify the user.
		if (!automatic) {
			switch (FINAL_ERROR_CODE) {
			case NO_ERROR_CODE:
				if (filesUploaded == 1) {
					makeToast(getResources().getString(
							R.string.uploader_no_errors_one_file),
							Toast.LENGTH_SHORT);
				} else {
					makeToast(
							filesUploaded
									+ " "
									+ getResources()
											.getString(
													R.string.uploader_no_errors_multiple_files),
							Toast.LENGTH_SHORT);
				}
				break;
			case UNKNOWNHOSTEXCEPTION_ERROR_CODE:
				makeToast(getResources().getString(
						R.string.uploader_unable_to_connect),
						Toast.LENGTH_SHORT);
				break;
			case UPLOAD_FAILED_ERROR_CODE:
				makeToast(getResources().getString(
						R.string.uploader_upload_failed), Toast.LENGTH_SHORT);
				break;
			case IOEXCEPTION_ERROR_CODE:
				makeToast(getResources().getString(
						R.string.uploader_upload_failed), Toast.LENGTH_SHORT);
				break;
			case NO_CONNECTION_ERROR:
				makeToast(getResources().getString(
						R.string.uploader_no_connection), Toast.LENGTH_SHORT);
				break;
			default:
				break;
			}
		}
		unregisterReceiver(completionReceiver);
	}

	/**
	 * Initializes preferences and the ConnectivityManager.
	 */
	private void setUpConnection() {
		wifiOnly = prefs.getBoolean(
				HSAndroidPreferences.UPLOAD_OVER_WIFI_ONLY_PREF, false);
		automatic = prefs.getBoolean(
				HSAndroidPreferences.AUTO_UPLOAD_DATA_PREF, false);

		connectivityMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * Cancels the timer.
	 */
	private void timerKill() {
		timer.cancel();
		timer = new Timer();
	}

	/**
	 * Starts the timer for reattempting file upload.
	 */
	private void timerStart() {
		final TimerTask task = new TimerTask() {
			@Override
			public void run() {
				uploadFiles();
			}
		};
		timer.schedule(task, DELAY);
	}

	/**
	 * Safely unregisters the ConnectReceiver.
	 */
	private void unregisterConnectReceiver() {
		if (connectionReceiverRegistered) {
			connectionReceiverRegistered = false;
			unregisterReceiver(connectReceiver);
		}
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

		if (files == null || files.length == 0) {
			return;
		} else {
			/*
			 * Add all files to a map so that we can keep track of which files
			 * were supposed to upload as the list is cleared.
			 */
			for (final String s : files) {
				if (!fileMap.containsKey(s)) {
					fileMap.put(s, null);
					fileList.add(UNUPLOADED_PATH + s);
				}
			}
		}
	}

	/**
	 * Uploads the specified file to the server.
	 * 
	 * @param fileName
	 *            The file to upload
	 * @return A code indicating the result of the upload.
	 */
	private int uploadFile(final String fileName) {
		TEMP_ERROR_CODE = NO_ERROR_CODE;
		Log.d(TAG, "Uploading " + fileName);
		httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(
				CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);

		httppost = new HttpPost(UPLOAD_URL);
		final File file = new File(Environment.getExternalStorageDirectory(),
				fileName);
		httppost.addHeader("MAC", wifiInfo.getMacAddress());
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
			return MALFORMEDURLEXCEPTION_ERROR_CODE;
		} catch (final UnknownHostException uhe) {
			Log.w(TAG, "Unable to connect...");
			FINAL_ERROR_CODE = UNKNOWNHOSTEXCEPTION_ERROR_CODE;
			return UNKNOWNHOSTEXCEPTION_ERROR_CODE;
		} catch (final IOException ioe) {
			Log.e(TAG, "error: " + ioe.getMessage(), ioe);
			FINAL_ERROR_CODE = IOEXCEPTION_ERROR_CODE;
			return IOEXCEPTION_ERROR_CODE;
		}
		return TEMP_ERROR_CODE;
	}

	/**
	 * Runs a new thread that uploads all files in the fileList.
	 */
	private void uploadFiles() {
		new Thread() {
			@Override
			public void run() {
				updateFileList();
				String ff = null;
				while (!fileList.isEmpty()) {
					if (canUpload()) {
						final String f = fileList.remove();
						final int retCode = uploadFile(f);
						if (retCode != NO_ERROR_CODE) {
							// In case of an IOException, the file is skipped.
							// Otherwise it is put back into the queue.
							if (retCode != IOEXCEPTION_ERROR_CODE) {
								fileList.addLast(f);
								if (ff == null) {
									ff = f;
								} else {
									// If we have gone through the whole list
									// and all files are giving errors, if on
									// automatic set the timer to try again,
									// else break and alert user that there were
									// errors.
									if (ff.equals(f)) {
										if (automatic) {
											waiting = true;
											// We are waiting to try the upload
											// for this file again after a set
											// time.
											timerStart();
											return;
										} else {
											break;
										}
									}
								}
							}
						} else {
							// If there was no error code, the file was
							// successfully uploaded.
							filesUploaded++;
						}
					} else {
						if (automatic) {
							waiting = true;
							// We now wait for a connection to become available,
							// so register a receiver for it.
							registerReceiver(connectReceiver, new IntentFilter(
									ConnectivityManager.CONNECTIVITY_ACTION));
							connectionReceiverRegistered = true;
							return;
						} else {
							FINAL_ERROR_CODE = NO_CONNECTION_ERROR;
							break;
						}
					}
				}
				fileMap.clear();
				fileList.clear();
				if (httpclient != null) {
					httpclient.getConnectionManager().shutdown();
				}
				final Intent i = new Intent();
				i.setAction(UPLOAD_COMPLETE_INTENT);
				sendBroadcast(i);
			}
		}.start();
	}

	/**
	 * Called when the wifi only preference is changed.
	 */
	private void wifiPrefChanged() {

		wifiOnly = prefs.getBoolean(
				HSAndroidPreferences.UPLOAD_OVER_WIFI_ONLY_PREF, false);
		// If we are on automatic uploads, wifi has been disabled, and we are
		// waiting for a connection to become available, then it's possible that
		// a 3G connection has been available the whole time so we try again.
		if (automatic && wifiOnly == false && waiting) {
			networkChanged();
		}
	}

}
