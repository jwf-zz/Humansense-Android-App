package ca.mcgill.hs.plugin;

import java.util.LinkedList;
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
	private int timeBetweenDiscoveries = 10000;

	//The Context in which the BluetoothLoggerReceiver will be registered.
	private final Context context;
	
	//The BluetoothLoggerReceiver from which we will get the bluetooth scan results.
	private BluetoothLoggerReceiver blr;
	
	//The BluetoothDiscoveryListener used to know when the discovery of bluetooth devices is completed.
	private BluetoothDiscoveryListener bdl;
	
	private final LinkedList<String> names;
	private final LinkedList<String> addresses;
	
	//Was the Bluetooth enable when the plugin was started
	private boolean wasEnabled = false;
	
	private boolean expectedInterrupt = false;
	
	private Thread exec;
	
	/**
	 * The default and only constructor for the BluetoothLogger InputPlugin.
	 * 
	 * @param context the required context to register the BluetoothLoggerReceiver.
	 */
	public BluetoothLogger(Context context){
		this.ba = BluetoothAdapter.getDefaultAdapter();
		if (ba != null){
			if (ba.isEnabled()){ wasEnabled = true; }
		}
		this.context = context;
		names = new LinkedList<String>();
		addresses = new LinkedList<String>();
	}
	
	public void startPlugin() {
		if (ba == null) return; //Device does not support Bluetooth
				
		blr = new BluetoothLoggerReceiver();
		context.registerReceiver(blr, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		Log.i("BluetoothLogger", "Registered logger receiver.");
		
		bdl = new BluetoothDiscoveryListener();
		context.registerReceiver(bdl, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		Log.i("BluetoothLogger", "Registered discovery listener.");
		
		exec = getExecutionThread();
		exec.start();
	}
	
	private void onDeviceFound(BluetoothDevice bd){
		names.add(bd.getName());
		addresses.add(bd.getAddress());
	}

	@Override
	public void stopPlugin() {
		if (ba == null) return;
		
		expectedInterrupt = true;
		exec.interrupt();
		
		context.unregisterReceiver(blr);
		Log.i("BluetoothLogger", "Unegistered receiver.");
		context.unregisterReceiver(bdl);
		Log.i("BluetoothLogger", "Unegistered discovery listener.");
		
		ba.cancelDiscovery();
		
		//TODO: Add user prompt
		if (!wasEnabled) ba.disable();
	}
	
	private Thread getExecutionThread(){
		Log.i("BluetoothThread", "Starting execution thread.");
		return new Thread(){
			public void run(){
				try {
					sleep(timeBetweenDiscoveries);
					if (ba != null){
						if (!ba.isEnabled()){
							Log.i("BluetoothThread", "Enabling BluetoothAdapter.");
							ba.enable();
							while (!ba.isEnabled()){}
						}
						Log.i("BluetoothThread", "Starting discovery.");
						ba.startDiscovery();
					}
				} catch (InterruptedException e) {
					if (expectedInterrupt) Log.e("BluetoothThread", "Expected thread interruption.");
					else e.printStackTrace();
				}
			}
		};
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothLoggerReceiver
	// ***********************************************************************************
	
	private class BluetoothLoggerReceiver extends BroadcastReceiver {
		
		public BluetoothLoggerReceiver() {
			super();
		}
		
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
			c.unregisterReceiver(this);
			write(new BluetoothPacket(System.currentTimeMillis(), names.size(), names, addresses));
			names.clear();
			addresses.clear();
			exec = getExecutionThread();
			exec.start();
			c.registerReceiver(this, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothPacket
	// ***********************************************************************************

	public class BluetoothPacket implements DataPacket{
		
		final long time;
		final int neighbours;
		final LinkedList<String> names;
		final LinkedList<String> addresses;
		
		public BluetoothPacket(long time, int neighbours, LinkedList<String> names, LinkedList<String> addresses){
			this.time = time;
			this.neighbours = neighbours;
			this.names = names;
			this.addresses = addresses;
		}

		public String getInputPluginName() {
			return "BluetoothLogger";
		}
		
		@SuppressWarnings("unchecked")
		public DataPacket clone(){
			return new BluetoothPacket(time, neighbours, (LinkedList<String>)names.clone(), (LinkedList<String>)addresses.clone());
		}
		
	}
}
