/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.uploader;

import java.util.Random;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NetworkHelper {
	private static final String TAG = "NetworkHelper";

	public static Random sRandom = new Random(SystemClock.uptimeMillis());

	private static String macAddress = null;

	public static String getMacAddress(final Context context) {
		if (macAddress == null) {
			macAddress = ((WifiManager) context
					.getSystemService(Context.WIFI_SERVICE))
					.getConnectionInfo().getMacAddress();
		}
		return macAddress;
	}

	/**
	 * Returns whether the network is available
	 */
	public static boolean isNetworkAvailable(final Context context) {
		final ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(TAG, "couldn't get connectivity manager");
		} else {
			final NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].isConnected()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns whether the network is roaming
	 */
	public static boolean isNetworkRoaming(final Context context) {
		final ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(TAG, "couldn't get connectivity manager");
		} else {
			final NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null
					&& info.getType() == ConnectivityManager.TYPE_MOBILE) {
				final TelephonyManager telephony = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
				if (telephony.isNetworkRoaming()) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isWifiNetworkAvailable(final Context context) {
		final ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(TAG, "couldn't get connectivity manager");
		} else {
			final NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null
					&& info.isConnected()
					&& (info.getType() == ConnectivityManager.TYPE_WIFI || info
							.getType() == ConnectivityManager.TYPE_WIMAX)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Only meant to be used statically.
	 */
	private NetworkHelper() {
	}

}
