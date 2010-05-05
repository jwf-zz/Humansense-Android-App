package ca.mcgill.hs.serv;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HSServAutoStart extends BroadcastReceiver{
	
	@Override
	public void onReceive(Context context, Intent intent){
		if( "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			ComponentName comp = new ComponentName(context.getPackageName(), HSService.class.getName());
			ComponentName svc = context.startService(new Intent().setComponent(comp));
			
			if (svc == null){
				Log.e("HSServAutoStart", "Could not start HSService " + comp.toString());
			}
		}
		
	}

}
