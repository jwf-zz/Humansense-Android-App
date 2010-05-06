package ca.mcgill.hs.serv;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class HSService extends Service{
	
	//integer counter
	private static int counter;
	private static boolean isRunning;
	private Timer timer = new Timer();
	private static long UPDATE_INTERVAL = 1000;
	
	/**
	 * Is the service running
	 */
	public static boolean isRunning(){
		return isRunning;
	}

	/**
	 * Setting for the frequency of updates
	 */
	public static void setUpdateInterval(long INTERVAL){
		UPDATE_INTERVAL = INTERVAL;
	}
	
	public static int getCounter(){
		return counter;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		Log.i(getClass().getSimpleName(), "Timer started!!!");
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if (timer != null) timer.cancel();
		Log.i(getClass().getSimpleName(), "Timer stopped!!!");
		isRunning = false;
				
		//show a quick toast to indicate stop of service
		Toast toast = Toast.makeText(getApplicationContext(), "Counter stopped!", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		super.onStart(intent, startId);
		timer.scheduleAtFixedRate(
				new TimerTask(){
					public void run(){
						counter++;
						Log.i(getClass().getSimpleName(), "Counter: "+counter);
					}
				}, 0, UPDATE_INTERVAL);
		isRunning = true;
		
		//show a quick toast to indicate start of service
		Toast toast = Toast.makeText(getApplicationContext(), "Counter started!", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}
