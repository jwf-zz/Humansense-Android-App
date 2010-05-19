package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Pipe.SinkChannel;
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
	private WritableByteChannel wbc;
	
	/**
	 * Constructor.
	 */
	public WifiLogger(WifiManager wm, Context context, WritableByteChannel wbc){
		this.wm = wm;
		this.context = context;
		this.wbc = wbc;
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
		ByteBuffer timestamp = ByteBuffer.allocate(8);
		ByteBuffer ssid, bssid, level;
		ByteBuffer[] packet = new ByteBuffer[4];
		for (ScanResult result : results) {
			timestamp.clear();
			timestamp.putLong(System.currentTimeMillis());
			timestamp.flip();
			//packet[0] = timestamp;
			//ssid = ByteBuffer.allocate(result.SSID.length()).put(result.SSID.getBytes());
			//packet[1] = ssid;
			//bssid = ByteBuffer.allocate(result.BSSID.length()).put(result.BSSID.getBytes());
			//packet[2] = bssid;
			//level = ByteBuffer.allocate(4).putInt(result.level);
			//packet[3] = level;
			try {
				//sc.write(packet);
				while (wbc.write(timestamp) > 0){}
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		
		private final WifiManager wifi;
	}

}
