package ca.mcgill.hs.serv;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.LinkedList;

import ca.mcgill.hs.plugin.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

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
	 * Called when the service is stopped. Stops the service.
	 */
	public void onDestroy(){
		super.onDestroy();
		
		for (InputPlugin plugin : inputPluginList) plugin.stopPlugin();
		for (OutputPlugin plugin : outputPluginList) plugin.closePlugin();
		
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
				
		//Start input plugins.
		for (InputPlugin plugin: inputPluginList) plugin.startPlugin();
		
		//Start output plugins.
		for (OutputPlugin plugin : outputPluginList) plugin.startPlugin();
		
		//Connect inout and output plugins.
		try {
			createConnections();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
	
	/**
	 * Populates the list of input plugins.
	 */
	private void addInputPlugins(){
		inputPluginList.add(new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),getBaseContext()));
	}
	
	/**
	 * Populates the list of output plugins.
	 */
	private void addOutputPlugins(){
		outputPluginList.add(new ScreenOutput());
	}
	
	/**
	 * Creates the connections betweeen the input and output plugins.
	 */
	private void createConnections() throws IOException{
		Class[] requestedClassList;
		for (InputPlugin input : inputPluginList){
			requestedClassList = input.getOutputClassList();
			for (int i = 0; i < requestedClassList.length; i++){
				for (OutputPlugin output : outputPluginList){
					if (output.getClass() == requestedClassList[i]){
						Log.i("HSService", "Connecting " + input.getClass().getSimpleName() + " and " + output.getClass().getSimpleName());
						Pipe p = Pipe.open();
						input.connect(p.sink());
						output.connect(p.source());
					}
				}
			}
		}
	}
}
