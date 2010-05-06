package ca.mcgill.hs.serv;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
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
		isRunning = false;
		Log.i(getClass().getSimpleName(), "Timer stopped!!!");
		
		//show a quick toast to verify start of service
		Context context = getApplicationContext();
		CharSequence text = "Counter stopped!";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
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
		
		//show a quick toast to verify start of service
		Context context = getApplicationContext();
		CharSequence text = "Counter started!";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}
