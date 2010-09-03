package ca.mcgill.hs.uploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Receives system broadcasts (boot, network connectivity)
 */
public class UploadReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			final NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if (info != null && info.isConnected()) {
				context.startService(new Intent(context, UploadService.class));
			}
		} else if (intent.getAction().equals(Constants.ACTION_RETRY)) {
			context.startService(new Intent(context, UploadService.class));
		}
	}
}
