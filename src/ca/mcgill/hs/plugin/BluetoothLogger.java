package ca.mcgill.hs.plugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothLogger extends InputPlugin{
	
	//The BluetoothAdapter used to start and stop discovery of devices.
	private final BluetoothAdapter ba;
	
	//The interval of time between two subsequent scans.
	private int timeBetweenDiscoveries = 0;
	
	//A boolean detailing whether or not the Thread is running.
	private boolean threadRunning = false;

	//The Context in which the BluetoothLoggerReceiver will be registered.
	private final Context context;
	
	//The BluetoothLoggerReceiver from which we will get the bluetooth scan results.
	private BluetoothLoggerReceiver blr;
	
	//The BluetoothDiscoveryListener used to know when the discovery of bluetooth devices is completed.
	private BluetoothDiscoveryListener bdl;
	
	/**
	 * The default and only constructor for the BluetoothLogger InputPlugin.
	 * 
	 * @param context the required context to register the BluetoothLoggerReceiver.
	 */
	public BluetoothLogger(Context context){
		this.ba = BluetoothAdapter.getDefaultAdapter();
		this.context = context;
	}
	
	public void startPlugin() {
		if (ba == null) return; //Device does not support Bluetooth
		
		if (!ba.isEnabled()) {
			//TODO: Add user prompt
			ba.enable();
		}
		
		blr = new BluetoothLoggerReceiver();
		context.registerReceiver(blr, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		Log.i("BluetoothLogger", "Registered logger receiver.");
		
		bdl = new BluetoothDiscoveryListener();
		context.registerReceiver(bdl, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		Log.i("BluetoothLogger", "Registered discovery listener.");
		
		threadRunning = true;
	}
	
	private void onDeviceFound(BluetoothDevice bd){
		write(new BluetoothPacket(System.currentTimeMillis(), bd.getName(), bd.getAddress()));
	}
	
	private void startDiscovery(){
		if (ba != null){
			if (!ba.isDiscovering()){
				ba.startDiscovery();
			}
		}
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
	// PRIVATE INNER CLASS -- BluetoothDiscoveryListener
	// ***********************************************************************************
	
	private class BluetoothDiscoveryListener extends BroadcastReceiver {
		
		public BluetoothDiscoveryListener() {
			super();
		}

		public void onReceive(Context c, Intent intent) {
			try {
				wait(timeBetweenDiscoveries);
				startDiscovery();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothPacket
	// ***********************************************************************************

	public class BluetoothPacket implements DataPacket{
		
		final long time;
		final String name;
		final String address;
		
		public BluetoothPacket(long time, String name, String address){
			this.time = time;
			this.name = name;
			this.address = address;
		}

		public String getInputPluginName() {
			return "BluetoothLogger";
		}
		
		public DataPacket clone(){
			return new BluetoothPacket(time, name, address);
		}
		
	}
}
