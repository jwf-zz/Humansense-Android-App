/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.uploader;

import java.io.File;
import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.HSAndroidPreferences;
import ca.mcgill.hs.prefs.PreferenceFactory;

/**
 * Work in progress. A new, more modular file uploader. Still does not work.
 * 
 */
public class UploadService extends Service {

	private class DirectoryObserver extends FileObserver {
		private boolean isWatching = false;

		public DirectoryObserver(final String path, final int mask) {
			super(path, mask);
		}

		public boolean getIsWatching() {
			return isWatching;
		}

		@Override
		public void onEvent(final int event, final String path) {
			pendingUpdate = true;
		}

		@Override
		public void startWatching() {
			super.startWatching();
			isWatching = true;
		}

		@Override
		public void stopWatching() {
			isWatching = false;
			super.stopWatching();
		}
	}

	private static class UpdateThread extends Thread {
		public UpdateThread() {
			super("Humansense Upload Service");
		}

		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

			long wakeUp = Long.MAX_VALUE;
			for (;;) {
				synchronized (UploadService.updateLock) {
					if (updateThread != this) {
						throw new IllegalStateException(
								"multiple UpdateThreads in UploadService");
					}
					if (!pendingUpdate) {
						updateThread = null;
						if (wakeUp != Long.MAX_VALUE) {
							final AlarmManager alarms = (AlarmManager) context
									.getSystemService(Context.ALARM_SERVICE);
							if (alarms == null) {
								Log.e(Constants.TAG,
										"couldn't get alarm manager");
							} else {
								Log.v(Constants.TAG, "scheduling retry in "
										+ wakeUp + "ms");
								final Intent intent = new Intent(
										Constants.ACTION_RETRY);
								intent.setClassName("ca.mcgill.hs.uploader",
										UploadReceiver.class.getName());
								alarms.set(AlarmManager.RTC_WAKEUP, System
										.currentTimeMillis()
										+ wakeUp, PendingIntent.getBroadcast(
										context, 0, intent,
										PendingIntent.FLAG_ONE_SHOT));
							}
						}
					}
					pendingUpdate = false;
				}
				final boolean networkAvailable = NetworkHelper
						.isNetworkAvailable(context);
				final boolean isWifiNetwork = NetworkHelper
						.isWifiNetworkAvailable(context);
				final long now = System.currentTimeMillis();
				wakeUp = Long.MAX_VALUE;

				final String[] allFiles = recentDir.list();
				for (final String file : allFiles) {
					// Check if our file is already in the uploadList
					boolean alreadySeen = false;
					for (final UploadInfo info : uploadList) {
						if (info.mFileName.equals(file)) {
							alreadySeen = true;
							break;
						}
					}
					if (!alreadySeen) {
						// New file, add to uploadList.
						insertUpload(new File(recentDir.getPath(), file),
								networkAvailable, isWifiNetwork, now);
					}
				}

			}
		}
	}

	private static boolean pendingUpdate;

	private static ArrayList<UploadInfo> uploadList;

	public static final String ACTION_UPLOAD_COMPLETED = "android.intent.action.UPLOAD_COMPLETED";

	private static final String TAG = "UploaderService";
	private static File recentDir;

	private static DirectoryObserver dirObserver;
	private static boolean onlyUploadOverWifi;

	private static boolean autoUpload;

	private static Context context;

	private static Object[] updateLock = new Object[0];

	private static SharedPreferences prefs;

	private static UpdateThread updateThread;

	public static final String AUTO_UPLOAD_CHANGED_INTENT = "ca.mcgill.hs.HSAndroidApp.AUTO_UPLOAD_CHANGED_INTENT";

	public static final String WIFI_ONLY_CHANGED_INTENT = "ca.mcgill.hs.HSAndroidApp.WIFI_ONLY_CHANGED_INTENT";

	public static void insertUpload(final File file,
			final boolean networkAvailable, final boolean isWifiNetwork,
			final long now) {
		final Uri uri = Uri.fromFile(file);
		final UploadInfo info = new UploadInfo(uri, file.getPath(), 0,
				Constants.STATUS_PENDING, now, 0, file.length(), 0);
		uploadList.add(info);
	}

	public static void updateUploadList() {
		synchronized (updateLock) {
			pendingUpdate = true;
			if (updateThread == null) {
				updateThread = new UpdateThread();
				updateThread.start();
			}
		}
	}

	/**
	 * Show a notification while this service is running.
	 */
	/*
	 * private void showNotification() { // In this sample, we'll use the same
	 * text for the ticker and the expanded notification CharSequence text =
	 * getText(R.string.local_service_started);
	 * 
	 * // Set the icon, scrolling text and timestamp Notification notification =
	 * new Notification(R.drawable.stat_sample, text,
	 * System.currentTimeMillis());
	 * 
	 * // The PendingIntent to launch our activity if the user selects this
	 * notification PendingIntent contentIntent =
	 * PendingIntent.getActivity(this, 0, new Intent(this,
	 * LocalServiceActivities.Controller.class), 0);
	 * 
	 * // Set the info for the views that show in the notification panel.
	 * notification.setLatestEventInfo(this,
	 * getText(R.string.local_service_label), text, contentIntent);
	 * 
	 * // Send the notification. // We use a layout id because it is a unique
	 * number. We use it later to cancel.
	 * mNM.notify(R.string.local_service_started, notification); }
	 */

	// BroadcastReceiver for Use Wifi only preference change
	private final BroadcastReceiver wifiOnlyPrefChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			onlyUploadOverWifi = prefs.getBoolean(
					HSAndroidPreferences.UPLOAD_OVER_WIFI_ONLY_PREF, false);
		}
	};

	// Broadcast receiver for automatic upload preference changed
	private final BroadcastReceiver autoPrefChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			autoUpload = prefs.getBoolean(
					HSAndroidPreferences.AUTO_UPLOAD_DATA_PREF, false);
			if ((dirObserver).getIsWatching() && !autoUpload) {
				// Auto-upload was turned off
				dirObserver.stopWatching();
			} else if (!dirObserver.getIsWatching() && autoUpload) {
				// Auto-upload was turned on
				dirObserver.startWatching();
				updateUploadList();
			}
		}
	};

	@Override
	public IBinder onBind(final Intent i) {
		throw new UnsupportedOperationException(
				"Cannot bind to Upload Manager Service");
	}

	@Override
	public void onCreate() {
		recentDir = new File(Environment.getExternalStorageDirectory(),
				(String) getResources().getText(R.string.recent_file_path));
		dirObserver = new DirectoryObserver(recentDir.getAbsolutePath(),
				FileObserver.MOVED_TO);
		prefs = PreferenceFactory.getSharedPreferences();
		registerReceiver(wifiOnlyPrefChanged, new IntentFilter(
				WIFI_ONLY_CHANGED_INTENT));
		registerReceiver(autoPrefChanged, new IntentFilter(
				AUTO_UPLOAD_CHANGED_INTENT));

		context = getApplicationContext();
		uploadList = new ArrayList<UploadInfo>();
	}

	@Override
	public void onDestroy() {
		if (dirObserver.isWatching) {
			dirObserver.stopWatching();
		}
	}

	@Override
	public void onStart(final Intent intent, final int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		onlyUploadOverWifi = prefs.getBoolean(
				HSAndroidPreferences.UPLOAD_OVER_WIFI_ONLY_PREF, false);
		autoUpload = prefs.getBoolean(
				HSAndroidPreferences.AUTO_UPLOAD_DATA_PREF, false);

		if (autoUpload) {
			// Watch the recent directory for new files
			dirObserver.startWatching();
			updateUploadList();
		}
	}
}
