package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Logs the Wifi data. Mostly a test plugin for now.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLogger implements InputPlugin{

	private Thread wifiLoggerThread;
	private boolean threadRunning = false;
	private WifiManager wm;
	private static int sleepIntervalMillisecs = 5000;
	private WifiLoggerReceiver wlr;
	private Context context;
	final private DataOutputStream dos;
	
	/**
	 * Constructor.
	 */
	public WifiLogger(WifiManager wm, Context context, DataOutputStream dos){
		this.wm = wm;
		this.context = context;
		this.dos = dos;
	}
	
	/**
	 * @override
	 */
	public void startPlugin() {
		
		wlr = new WifiLoggerReceiver(wm);
		context.registerReceiver(wlr, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i("WifiLogger", "Registered receiver.");
		
		wifiLoggerThread = new Thread() {
			public void run() {
				try {
					while(threadRunning) {
						Log.i("WifiLogger", "Scanning results.");
						wm.startScan();
						sleep(sleepIntervalMillisecs);
					}
				}
				catch(InterruptedException e) {
					Log.d("WifiLogger", "Logging thread terminated.");
				}
			}
		};
		wifiLoggerThread.start();
		threadRunning = true;
	}

	/**
	 * @override
	 */
	public void stopPlugin() {
		threadRunning = false;
		Log.i("WifiLogger", "Thread wuz killed by DJ Werd.");
		context.unregisterReceiver(wlr);
		Log.i("WifiLogger", "Unegistered receiver.");
	}
	
	/**
	 * Processes the results sent by the Wifi scan and writes them to the
	 * DataOutputStream.
	 * @throws IOException 
	 */
	private void processResults(List<ScanResult> results){
		try {
			for (ScanResult result : results) {
				dos.writeLong(System.currentTimeMillis());
				dos.writeUTF(result.SSID);
				dos.writeUTF(result.BSSID);
				dos.writeInt(result.level);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- WifiLoggerReceiver
	// ***********************************************************************************
	
	/**
	 * Taken from Jordan Frank (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and
	 * modified for this plugin.
	 */
	private class WifiLoggerReceiver extends BroadcastReceiver {
		
		public WifiLoggerReceiver(WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		@Override
		public void onReceive(Context c, Intent intent) {
			final List<ScanResult> results = wifi.getScanResults();
			Log.i("WifiLogger", "Recieved wifi results.");
			processResults(results);
		}
		
		private static final String TAG = "WifiLocationLogger";
		private final WifiManager wifi;
	}

}
