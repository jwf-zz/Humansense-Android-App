package ca.mcgill.hs.plugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothLogger extends InputPlugin{
	
	private final BluetoothAdapter ba;
	
	//The interval of time between two subsequent scans.
	private int sleepIntervalMillisecs = 30000;
	
	//The Thread for requesting scans.
	private Thread bluetoothThread;
	
	//A boolean detailing whether or not the Thread is running.
	private boolean threadRunning = false;

	//The Context in which the BluetoothLoggerReceiver will be registered.
	private final Context context;
	
	//The BluetoothLoggerReceiver from which we will get the bluetooth scan results.
	private BluetoothLoggerReceiver blr;
	
	public BluetoothLogger(Context context){
		this.ba = BluetoothAdapter.getDefaultAdapter();
		this.context = context;
	}

	@Override
	public void startPlugin() {
		if (ba == null) return; //Device does not support Bluetooth
		
		if (!ba.isEnabled()) {
			//TODO: Add user prompt
			ba.enable();
		}
		
		blr = new BluetoothLoggerReceiver();
		context.registerReceiver(blr, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		Log.i("BluetoothLogger", "Registered receiver.");
		
		bluetoothThread = new Thread() {
			public void run() {
				try {
					while(threadRunning) {
						if (ba.isDiscovering()){
							ba.cancelDiscovery();
							Log.i("BluetoothLogger", "Stopped current discovery.");
						}
						Log.i("BluetoothLogger", "Checking for devices...");
						ba.startDiscovery();
						sleep(sleepIntervalMillisecs);
					}
				}
				catch(InterruptedException e) {
					Log.e("BluetoothLogger", "Logging thread terminated due to InterruptedException.");
				}
			}
		};
		
		bluetoothThread.start();
		threadRunning = true;
	}
	
	private void onDeviceFound(BluetoothDevice bd){
		write(new BluetoothPacket(bd.getName(), bd.getAddress()));
	}

	@Override
	public void stopPlugin() {
		if (ba == null) return;
		
		if (threadRunning){
			//TODO: Add user prompt
			ba.disable();
			threadRunning = false;
			context.unregisterReceiver(blr);
			Log.i("BluetoothLogger", "Unegistered receiver.");
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothLoggerReceiver
	// ***********************************************************************************
	
	private class BluetoothLoggerReceiver extends BroadcastReceiver {
		
		public BluetoothLoggerReceiver() {
			super();
		}

		@Override
		public void onReceive(Context c, Intent intent) {
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			onDeviceFound(device);
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothPacket
	// ***********************************************************************************

	public class BluetoothPacket implements DataPacket{
		
		public final String name;
		public final String address;
		
		public BluetoothPacket(String name, String address){
			this.name = name;
			this.address = address;
		}

		public String getInputPluginName() {
			return "BluetoothLogger";
		}
		
		public DataPacket clone(){
			return new BluetoothPacket(name, address);
		}
		
	}
}
