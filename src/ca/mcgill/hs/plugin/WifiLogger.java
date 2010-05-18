package ca.mcgill.hs.plugin;

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
	
	/**
	 * @override
	 */
	public void startPlugin() {
		wifiLoggerThread = new Thread() {
			public void run() {
				try {
					while(threadRunning) {
						Log.i("WifiLogger", "Thread code wuz here.");
						sleep(5000); //sleep 5 seconds
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
	}

}
