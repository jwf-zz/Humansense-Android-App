package ca.mcgill.hs.plugin;

import java.util.LinkedList;

import ca.mcgill.hs.R;
import ca.mcgill.hs.util.PreferenceFactory;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

public class BluetoothLogger extends InputPlugin{
	
	//The BluetoothAdapter used to start and stop discovery of devices.
	private final BluetoothAdapter ba;
	
	//Boolean ON-OFF switch *Temporary only*
	private final boolean PLUGIN_ACTIVE;
	
	//The interval of time between two subsequent scans.
	private int timeBetweenDiscoveries;

	//The Context in which the BluetoothLoggerReceiver will be registered.
	private final Context context;
	
	//The BluetoothLoggerReceiver from which we will get the bluetooth scan results.
	private BluetoothLoggerReceiver blr;
	
	//The BluetoothDiscoveryListener used to know when the discovery of bluetooth devices is completed.
	private BluetoothDiscoveryListener bdl;
	
	//Lists holding results.
	private final LinkedList<String> names;
	private final LinkedList<String> addresses;
	
	//Was the Bluetooth enable when the plugin was started
	private boolean wasEnabled = false;
	
	//If this is true, the BluetoothThread is interrupted and an expected InterruptedException is caught.
	private boolean expectedInterrupt = false;
	
	//The main BluetoothThread for this plugin.
	private Thread exec;
	
	//If true, the Bluetooth adapter will be automatically enabled when the service is started.
	private final boolean forceBluetoothActivation;
	
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
		
		SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(context);
		
		forceBluetoothActivation = prefs.getBoolean("forceBluetoothOn", false);
		
		timeBetweenDiscoveries = Integer.parseInt(prefs.getString("bluetoothLoggerTimeInterval", "60000"));
		
		PLUGIN_ACTIVE = prefs.getBoolean("bluetoothLoggerEnable", false);
	}
	
	@Override
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) return;
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
	
	/**
	 * Called when a device is found. Adds the name and address of the found device to the lists of names/addresses
	 * found during this scan.
	 * @param bd the BluetoothDevice that was found.
	 */
	private void onDeviceFound(BluetoothDevice bd){
		if (bd.getName() == null) return;
		names.add(bd.getName());
		addresses.add(bd.getAddress());
	}
	
	/**
	 * Returns true if this plugin has preferences, and false otherwise.
	 * @return a boolean representing whether or not this plugin has preferences.
	 * 
	 * @override
	 */
	public static boolean hasPreferences() {return true;}
	
	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 * 
	 * @override
	 */
	public static Preference[] getPreferences(Context c){
		Preference[] prefs = new Preference[3];
		
		prefs[0] = PreferenceFactory.getCheckBoxPreference(c, "bluetoothLoggerEnable",
				"BT Plugin", "Enables or disables this plugin.",
				"BluetoothLogger is on.", "BluetoothLogger is off.");
		
		prefs[1] = PreferenceFactory.getCheckBoxPreference(c, "forceBluetoothOn",
				"BT Auto-Enable", "Auto-Enables the Bluetooth adapter when logging is started.",
				"Bluetooth will be Auto-Enabled when logging is started.", "Bluetooth will not be Auto-Enabled when logging is started.");
	
		prefs[2] = PreferenceFactory.getListPreference(c, R.array.bluetoothLoggerIntervalStrings,
				R.array.bluetoothLoggerIntervalValues, "60000", "bluetoothLoggerTimeInterval",
				R.string.bluetoothlogger_interval_pref, R.string.bluetoothlogger_interval_pref_summary);
		
		return prefs;
	}

	/**
	 * Stops the plugin, interrupts the execution thread, unregisters all broadcast receivers and
	 * cancels any ongoing discoveries. Disables Bluetooth adapter if it was disabled when the service was started.
	 * 
	 * @override
	 */
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) return;
		if (ba == null) return;
		
		expectedInterrupt = true;
		exec.interrupt();
		
		context.unregisterReceiver(blr);
		Log.i("BluetoothLogger", "Unegistered receiver");
		context.unregisterReceiver(bdl);
		Log.i("BluetoothLogger", "Unegistered discovery listener");
		
		ba.cancelDiscovery();
		
		if (!wasEnabled) ba.disable();
	}
	
	/**
	 * Returns a new thread that will serve as the execution thread for this plugin.
	 * @return the execution thread for this plugin.
	 */
	private Thread getExecutionThread(){
		Log.i("BluetoothThread", "Starting execution thread");
		return new Thread(){
			public void run(){
				try {
					sleep(timeBetweenDiscoveries);
					if (ba != null){
						if (!ba.isEnabled()){
							if (forceBluetoothActivation){
								ba.enable();
								Log.i("BluetoothThread", "Enabling Bluetooth Adapter");
							}				
							while (!ba.isEnabled()){}
						}
						Log.i("BluetoothThread", "Starting discovery");
						ba.startDiscovery();
					}
				} catch (InterruptedException e) {
					if (expectedInterrupt) Log.e("BluetoothThread", "Expected thread interruption");
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
		
		@Override
		public void onReceive(Context c, Intent intent) {
			c.unregisterReceiver(this);
			if (names.size()>0 && addresses.size()>0){
				write(new BluetoothPacket(System.currentTimeMillis(), names.size(), names, addresses));
				names.clear();
				addresses.clear();
			}
			exec = getExecutionThread();
			exec.start();
			c.registerReceiver(this, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		}
	}
	
	// ***********************************************************************************
	// PRIVATE INNER CLASS -- BluetoothPacket
	// ***********************************************************************************

	public static class BluetoothPacket implements DataPacket{
		
		final long time;
		final int neighbours;
		final LinkedList<String> names;
		final LinkedList<String> addresses;
		final static String PLUGIN_NAME = "BluetoothLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

		public BluetoothPacket(long time, int neighbours, LinkedList<String> names, LinkedList<String> addresses){
			this.time = time;
			this.neighbours = neighbours;
			this.names = names;
			this.addresses = addresses;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public DataPacket clone(){
			return new BluetoothPacket(time, neighbours, (LinkedList<String>)names.clone(), (LinkedList<String>)addresses.clone());
		}

		@Override
		public int getDataPacketId() {
			return BluetoothPacket.PLUGIN_ID;
		}

		@Override
		public String getInputPluginName() {
			return BluetoothPacket.PLUGIN_NAME;
		}
		
	}
}
