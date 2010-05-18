package ca.mcgill.hs.serv;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
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
		for (OutputPlugin plugin : outputPluginList) plugin.stopPlugin();
		
		isRunning = false;
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		//Instantiate input plugins.
		//WifiLogger
		final PipedOutputStream wifiLoggerPOS = new PipedOutputStream();
		final PipedInputStream wifiLoggerPIS = new PipedInputStream();
		try {
			wifiLoggerPOS.connect(wifiLoggerPIS);
		} catch (IOException e) {
			e.printStackTrace();
		}
		WifiLogger wl = new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),getBaseContext(),new DataOutputStream(wifiLoggerPOS));
		inputPluginList.add(wl);
		
		//Instantiate output plugins.
		//FileOutput
		FileOutput fo = new FileOutput(wifiLoggerPIS);
		outputPluginList.add(fo);
		
		//Start input plugins.
		for (InputPlugin plugin: inputPluginList) plugin.startPlugin();
		
		//Start output plugins.
		for (OutputPlugin plugin: outputPluginList) plugin.startPlugin();
		
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
}
