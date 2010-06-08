package ca.mcgill.hs.serv;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.util.LinkedList;

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
	final private LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	final private LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
		
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
		
		isRunning = false;
	}
	
	/**
	 * Called automatically when onCreate() is called. Initialises the service and associated plug-ins and starts the service.
	 */
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		//Instantiate input plugins.
		addInputPlugins();
		
		//Instantiate output plugins
		addOutputPlugins();
		
		try {
			createConnections();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
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
		inputPluginList.add(new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),getBaseContext()));
		inputPluginList.add(new GPSLocationLogger((LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE)));
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
	 * Creates the connections betweeen the input and output plugins.
	 */
	private void createConnections() throws IOException{
		for (InputPlugin input : inputPluginList){
			for (OutputPlugin output : outputPluginList){
				Pipe p = Pipe.open();
				ObjectOutputStream oos = new ObjectOutputStream(Channels.newOutputStream(p.sink()));
				ObjectInputStream ois = new ObjectInputStream(Channels.newInputStream(p.source()));
				input.connect(oos);
				output.connect(ois);
			}
		}
	}
	
}
