/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.serv;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Used to start the service when the phone boots up, by listening for the
 * BOOT_COMPLETED intent.
 */

public class HSServAutoStart extends BroadcastReceiver {

	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";
	private ComponentName comp;
	private ComponentName svc;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		// check the user settings to see if we should start the service at boot
		final SharedPreferences prefs = context.getSharedPreferences(
				"ca.mcgill.hs_preferences", 0);

		if (prefs.getBoolean("autoStartAtPhoneBoot", false)) {
			// check if the received intent is BOOT_COMPLETED
			if ("android.intent.action.BOOT_COMPLETED".equals(intent
					.getAction())) {
				comp = new ComponentName(context.getPackageName(),
						HSService.class.getName());
				svc = context.startService(new Intent().setComponent(comp));

				if (svc == null) {
					Log.e("HSServAutoStart", "Could not start HSService "
							+ comp.toString());
				}
			}
		}
	}

}
