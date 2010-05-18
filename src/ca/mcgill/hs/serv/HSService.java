package ca.mcgill.hs.serv;

import ca.mcgill.hs.plugin.*;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HSService extends Service{
	
	private static boolean isRunning;
	private InputPlugin testSingletonPlugin;
	
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
		
		testSingletonPlugin.stopPlugin();
		
		isRunning = false;
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		//Start your plugins!!
		testSingletonPlugin = new WifiLogger();
		testSingletonPlugin.startPlugin();
		
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
}
