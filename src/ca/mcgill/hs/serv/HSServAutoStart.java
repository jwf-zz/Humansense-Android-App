package ca.mcgill.hs.serv;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HSServAutoStart extends BroadcastReceiver{
	
	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";
	private ComponentName comp;
	private ComponentName svc;
	
	
	@Override
	public void onReceive(Context context, Intent intent){
		//check if the received intent is BOOT_COMPLETED
		if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) && new File("/sdcard/autoStartAtPhoneBoot.prfs").exists()) {
			comp = new ComponentName(context.getPackageName(), HSService.class.getName());
			svc = context.startService(new Intent().setComponent(comp));
						
			if (svc == null){
				Log.e("HSServAutoStart", "Could not start HSService " + comp.toString());
			}
		}
		
	}

}
