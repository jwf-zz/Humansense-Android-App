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
		ByteBuffer packet = ByteBuffer.allocate(0);
		
		for (ScanResult result : results) {
			//prepare buffer
			packet.clear();
			packet.position(0);
			
			//read info
			packet = ByteBuffer.allocate(8 + 4 + (2*result.SSID.length()) + 4 + (2*result.BSSID.length()) + 4);
			packet.putLong(System.currentTimeMillis());
			packet.putInt(result.level);
			packet.putInt(result.SSID.length());
			for (int i = 0; i < result.SSID.length(); i++) packet.putChar(result.SSID.charAt(i));
			packet.putInt(result.BSSID.length());
			for (int i = 0; i < result.BSSID.length(); i++) packet.putChar(result.BSSID.charAt(i));
			
			//flip & send
			packet.flip();
			try {
				while (wbc.write(packet) > 0){}
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
