package ca.mcgill.hs.serv;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import ca.mcgill.hs.prefs.HSAndroidPreferences;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

public class LowBatteryMonitor extends BroadcastReceiver {
	private static final String TAG = "LowBatteryMonitor";

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "Received Intent: " + intent.getAction());
		final SharedPreferences prefs = PreferenceFactory
				.getSharedPreferences(context);
		final boolean watchForLowBattery = prefs.getBoolean(
				HSAndroidPreferences.WATCH_FOR_LOW_BATTERY_PREF, true);
		if (watchForLowBattery) {
			final ComponentName comp = new ComponentName(context
					.getPackageName(), HSService.class.getName());
			if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())
					&& HSService.isRunning()) {
				final boolean svc = context.stopService(new Intent()
						.setComponent(comp));
				if (svc) {
					Log.d(TAG, "Service stopped on low battery.");
				} else {
					Log.e(TAG, "Could not stop service on low battery.");
				}
			} else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())
					&& !HSService.isRunning()) {
				final ComponentName svc = context.startService(new Intent()
						.setComponent(comp));
				if (svc != null) {
					Log.d(TAG, "Service started since battery is now charged.");
				} else {
					Log.e(TAG, "Could not start service after battery charge.");
				}
			}
		}
	}
}
