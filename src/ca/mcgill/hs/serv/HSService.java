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

public class HSService extends Service{
	
	private static boolean isRunning;
	final private LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	final private LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	
	/**
	 * Is the service running
	 */
	public static boolean isRunning(){
		return isRunning;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		for (InputPlugin plugin : inputPluginList) plugin.stopPlugin();
		for (OutputPlugin plugin : outputPluginList) plugin.closePlugin();
		
		isRunning = false;
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		//Instantiate input plugins.
		//WifiLogger
		Pipe wifiLoggerPipe = null;
		try{
			wifiLoggerPipe = Pipe.open();
			WifiLogger wl = new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),getBaseContext(),wifiLoggerPipe.sink());
			inputPluginList.add(wl);
		} catch (IOException ioe) {
			ioe.printStackTrace(System.err);
		}
		
		//Instantiate output plugins.
		//ScreenOutput
		ScreenOutput so = new ScreenOutput();
		if (wifiLoggerPipe != null) so.connect(wifiLoggerPipe.source());
		outputPluginList.add(so);
				
		//Start input plugins.
		for (InputPlugin plugin: inputPluginList) plugin.startPlugin();
		
		//Start output plugins.
		for (OutputPlugin plugin : outputPluginList) plugin.startPlugin();
		
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
}
