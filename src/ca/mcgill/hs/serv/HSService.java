package ca.mcgill.hs.serv;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.mcgill.hs.plugin.*;
import ca.mcgill.hs.util.PreferenceFactory;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class HSService extends Service{
	
	private static boolean isRunning;
	private Context PASSABLE_CONTEXT;
	
	//Lists of the plugins currently enabled.
	private static final LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	private static final LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	
	// Some variables for calculating performance metrics
	private static long timeSpentInOnDataReady = 0L;
	private static int numCallsToOnDataReady = 0;
	
	//A simple static array of the input plugin class names.
	public static final Class<?>[] inputPluginsAvailable = {
		BluetoothLogger.class,
		GPSLogger.class,
		GSMLogger.class,
		SensorLogger.class,
		WifiLogger.class
		};
	//A simple static array of the output plugin class names.
	public static final Class<?>[] outputPluginsAvailable = {
		FileOutput.class,
		ScreenOutput.class
		};
	
	//ExecutorService
	private static final ExecutorService tpe = Executors.newCachedThreadPool();
	
	// This is a BroadcastReceiver in order to signal to the plugins that a preference has changed.
	private static final BroadcastReceiver prefReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			preferencesChangedIntentReceived();
		}
	};
		
	/**
	 * Returns a boolean indicating if the service is running or not.
	 * @return true if the service is running and false otherwise.
	 */
	public static boolean isRunning(){
		return isRunning;
	}

	@Override
	//Unused
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Called when the service is started. Creates the service.
	 */
	public void onCreate(){
		super.onCreate();
	}
	
	/**
	 * Called when the service is stopped. Also stops all plugins.
	 */
	public void onDestroy(){
		super.onDestroy();
		
		for (InputPlugin plugin : inputPluginList) plugin.stopPlugin();
		for (OutputPlugin plugin : outputPluginList) plugin.stopPlugin();
		
		inputPluginList.clear();
		outputPluginList.clear();
		
		isRunning = false;
	}
	
	/**
	 * Called automatically when onCreate() is called. Initialises the service and associated plug-ins and starts the service.
	 */
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		PASSABLE_CONTEXT = getApplicationContext();
		
		//Register the receiver for when the preferences change.
		getApplicationContext().registerReceiver(prefReceiver, new IntentFilter(PreferenceFactory.PREFERENCES_CHANGED_INTENT));
		
		//Instantiate input plugins.
		addInputPlugins();
		
		//Instantiate output plugins
		addOutputPlugins();
				
		//Start input plugins.
		for (InputPlugin plugin: inputPluginList) plugin.startPlugin();
		
		//Start output plugins.
		for (OutputPlugin plugin : outputPluginList) plugin.startPlugin();
						
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
	
	/**
	 * Populates the list of input plugins.
	 */
	private void addInputPlugins(){
		inputPluginList.add(new BluetoothLogger(PASSABLE_CONTEXT));
		inputPluginList.add(new GPSLogger((LocationManager) getSystemService(Context.LOCATION_SERVICE), PASSABLE_CONTEXT));
		inputPluginList.add(new GSMLogger((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE), PASSABLE_CONTEXT));
		inputPluginList.add(new SensorLogger((SensorManager)getSystemService(Context.SENSOR_SERVICE), PASSABLE_CONTEXT));
		inputPluginList.add(new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),PASSABLE_CONTEXT));
	}
	
	/**
	 * Populates the list of output plugins.
	 */
	private void addOutputPlugins(){
		outputPluginList.add(new ScreenOutput(PASSABLE_CONTEXT));
		outputPluginList.add(new FileOutput(PASSABLE_CONTEXT));
	}
	
	/**
	 * Called when there is a DataPacket available from an InputPlugin.
	 * @param dp the DataPacket that is ready to be received.
	 * @param source the InputPlugin that created the DataPacket.
	 */
	public static void onDataReady(DataPacket dp, InputPlugin source){
		long currentTime = System.currentTimeMillis();
		for (OutputPlugin op : outputPluginList){
			op.onDataReady(dp.clone());
			tpe.execute(op);
		}
		timeSpentInOnDataReady += (System.currentTimeMillis() - currentTime);
		numCallsToOnDataReady += 1;
		if (numCallsToOnDataReady % 1000 == 0) {
			float avgTime = (float)timeSpentInOnDataReady/(float)numCallsToOnDataReady;
			Log.d("PERFORMANCE:", "Average time for onDataReady is " + avgTime + " milliseconds");
			timeSpentInOnDataReady = 0L;
			numCallsToOnDataReady = 0;
		}
	}
	
	/**
	 * This method is called whenever a preference has been changed. It signals all plugins that a
	 * preference has changed.
	 */
	private static void preferencesChangedIntentReceived(){
		for (InputPlugin p : inputPluginList) p.onPreferenceChanged();
		for (OutputPlugin p : outputPluginList) p.onPreferenceChanged();
	}
		
}
