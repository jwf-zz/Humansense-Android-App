package ca.mcgill.hs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ServiceOne extends Service{

	@Override
	public IBinder onBind(Intent intent) {
		// A very basic Service.
		return null;
	}

}
