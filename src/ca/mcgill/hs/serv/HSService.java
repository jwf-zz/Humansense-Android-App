package ca.mcgill.hs.serv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HSService extends Service{
	
	private static boolean isRunning;
	
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
		isRunning = false;
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
}
