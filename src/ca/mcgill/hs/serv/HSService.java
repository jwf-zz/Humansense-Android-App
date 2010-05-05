package ca.mcgill.hs.serv;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HSService extends Service{
	
	//integer counter
	private static int counter;
	
	private Timer timer = new Timer();
	private static final long UPDATE_INTERVAL = 5000;

	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		_startService();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	private void _startService(){
		timer.schedule(
				new TimerTask(){
					public void run(){
						counter++;
					}
				}, 0, UPDATE_INTERVAL);
	}

}
