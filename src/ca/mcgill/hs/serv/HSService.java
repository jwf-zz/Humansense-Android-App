package ca.mcgill.hs.serv;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HSService extends Service{
	
	//integer counter
	private static int counter;
	public static boolean isRunning;
	private Timer timer = new Timer();
	private static final long UPDATE_INTERVAL = 1000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		//Initialise the service
		_startService();
		Log.i(getClass().getSimpleName(), "Timer started!!!");
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		_stopService();
		Log.i(getClass().getSimpleName(), "Timer stopped!!!");
	}
	
	private void _startService(){
		timer.scheduleAtFixedRate(
				new TimerTask(){
					public void run(){
						counter++;
						Log.i(getClass().getSimpleName(), "Counter: "+counter);
					}
				}, 0, UPDATE_INTERVAL);
		isRunning = true;
	}
	
	private void _stopService() {
		  if (timer != null) timer.cancel();
		  isRunning = false;
	}
	
}