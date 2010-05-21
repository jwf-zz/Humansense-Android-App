package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
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
	private final LinkedList<WritableByteChannel> channelList = new LinkedList<WritableByteChannel>();
	
	private final Class[] OUTPUT_CLASS_LIST = { ScreenOutput.class };
	
	/**
	 * This is the basic constructor for the WifiLogger plugin. It has to be instantiated
	 * before it is started, and needs to be passed a reference to a WifiManager, a Context
	 * and a WritableByteChannel (java.nio).
	 * 
	 * @param wm - the WifiManager for this WifiLogger.
	 * @param context - the context in which this plugin is created.
	 * @param wbc - the WritableByteChannel through which data will be written.
	 */
	public WifiLogger(WifiManager wm, Context context){
		this.wm = wm;
		this.context = context;
	}
	
	/**
	 * This method starts the WifiLogger plugin and launches all appropriate threads. It
	 * also registers a new WifiLoggerReceiver to scan for possible network connections.
	 * 
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
					Log.d("WifiLogger", "Logging thread terminated due to InterruptedException.");
				}
			}
		};
		wifiLoggerThread.start();
		threadRunning = true;
	}

	/**
	 * This method stops the thread if it is running, and does nothing if it is not.
	 * 
	 * @override
	 */
	public void stopPlugin() {
		if (threadRunning){
			threadRunning = false;
			context.unregisterReceiver(wlr);
			Log.i("WifiLogger", "Unegistered receiver.");
		}
	}
	
	/**
	 * Processes the results sent by the Wifi scan and writes them to the
	 * DataOutputStream.
	 * 
	 * @throws IOException 
	 */
	private void processResults(List<ScanResult> results){
		ByteBuffer packet = ByteBuffer.allocate(0);
		
		for (ScanResult result : results) {
			//prepare buffer
			packet.clear();
			
			//read info
			packet = ByteBuffer.allocate(1 + 4 + 8 + 4 + 4 + (2*result.SSID.length()) + 4 + (2*result.BSSID.length()));
			packet.putInt(8 + 4 + 4 + (2*result.SSID.length()) + 4 + (2*result.BSSID.length()));
			packet.putLong(System.currentTimeMillis());
			packet.putInt(result.level);
			packet.putInt(result.SSID.length());
			for (int i = 0; i < result.SSID.length(); i++) packet.putChar(result.SSID.charAt(i));
			packet.putInt(result.BSSID.length());
			for (int i = 0; i < result.BSSID.length(); i++) packet.putChar(result.BSSID.charAt(i));
			
			//flip & send
			packet.flip();
			for (WritableByteChannel wbc : channelList){
				try {
					wbc.write(packet);
					packet.position(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	/**
	 * Adds this WritableByteChannel to this plugin's list.
	 * @param wbc the specified WritableByteChannel.
	 * @override
	 */
	public boolean connect(WritableByteChannel wbc) {
		channelList.add(wbc);
		return false;
	}
	
	/**
	 * Returns the list of output plugins this input plugin will want to write data to.
	 * @return the list of output plugins this input plugin will want to write data to.
	 * @override
	 */
	public Class[] getOutputClassList() {
		return OUTPUT_CLASS_LIST;
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
