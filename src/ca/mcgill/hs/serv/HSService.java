package ca.mcgill.hs.serv;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ca.mcgill.hs.plugin.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;

public class HSService extends Service{
	
	private static boolean isRunning;
	private Context PASSABLE_CONTEXT;
	private static final LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	private static final LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	
	//A simple static array of the input plugin class names.
	public static final Class<?>[] inputPluginsAvailable = {
		WifiLogger.class,
		GPSLogger.class,
		SensorLogger.class
		};
	//A simple static array of the output plugin class names.
	public static final Class<?>[] outputPluginsAvailable = {
		ScreenOutput.class,
		FileOutput.class
		};
	
	//Thread Pool Executor
	private static final int CORE_THREADS = inputPluginsAvailable.length * outputPluginsAvailable.length;
	private static final int MAX_THREADS = inputPluginsAvailable.length * outputPluginsAvailable.length;
	private static final int KEEP_ALIVE_TIME = 100;
	private static final ThreadPoolExecutor tpe = new ThreadPoolExecutor(CORE_THREADS,
			//Above: number of core threads to keep alive
			MAX_THREADS, //Max number of threads
			KEEP_ALIVE_TIME, //time that excess idle threads will wait for new tasks before terminating
			TimeUnit.MILLISECONDS, //The time unit for above
			new LinkedBlockingQueue<Runnable>()); //the queue to use for holding tasks before they are executed
		
	/**
	 * Returns a boolean indicating if the service is running or not.
	 * @return true if the service is running and false otherwise.
	 */
	public static boolean isRunning(){
		return isRunning;
	}

	@Override
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
		inputPluginList.add(new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),PASSABLE_CONTEXT));
		inputPluginList.add(new GPSLogger((LocationManager) getSystemService(Context.LOCATION_SERVICE), PASSABLE_CONTEXT));
		inputPluginList.add(new SensorLogger((SensorManager)getSystemService(Context.SENSOR_SERVICE)));
	}
	
	/**
	 * Populates the list of output plugins.
	 */
	private void addOutputPlugins(){
		outputPluginList.add(new ScreenOutput());
		outputPluginList.add(new FileOutput());
	}
	
	/**
	 * Called when there is a DataPacket available from an InputPlugin.
	 * @param dp the DataPacket that is ready to be received.
	 * @param source the InputPlugin that created the DataPacket.
	 */
	public static void onDataReady(DataPacket dp, InputPlugin source){
		for (OutputPlugin op : outputPluginList){
			op.onDataReady(dp.clone());
			tpe.execute(op);
		}
	}
		
}
