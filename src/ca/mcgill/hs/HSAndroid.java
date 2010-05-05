/**
 * 
 */
package ca.mcgill.hs;

import ca.mcgill.hs.serv.HSService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class HSAndroid extends Activity{
	
	private Button button;
	private Button autoAppStart;
	private Button autoPhoneBoot;
	private TextView tv;
	private Intent i;
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tv = (TextView) findViewById(R.id.counterText);
        
        //Intent
        i = new Intent(this, HSService.class);
        
        //Preferences
        settings = getSharedPreferences(HSANDROID_PREFS_NAME, Activity.MODE_WORLD_READABLE);
        editor = settings.edit();
        if (!settings.contains("Frequency")) editor.putInt("Frequency", 1000);
        if (!settings.contains("StartAtPhoneBoot"))editor.putBoolean("StartAtPhoneBoot", false);
        if (!settings.contains("StartAtAppStart"))editor.putBoolean("StartAtAppStart", false);
        editor.commit();
        
        //Application Auto Start
        if (settings.getBoolean("StartAtAppStart", false)){
        	startService(i);
        }
        
        //Buttons
        button = (Button) findViewById(R.id.button);
        button.setText((HSService.isRunning() ? R.string.stop_label : R.string.start_label));
        button.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!HSService.isRunning()){ //NOT RUNNING
					startService(i);
					button.setText(R.string.stop_label);
				} else { //RUNNING
					stopService(i);
					button.setText(R.string.start_label);
					tv.setText("Counter = " + HSService.getCounter());
				}
			}
		});
        
        autoAppStart = (Button) findViewById(R.id.appStart);
        autoAppStart.setText(settings.getBoolean("StartAtAppStart", false) ? R.string.no_start_app_start_label : R.string.start_app_start_label);
        autoAppStart.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (settings.getBoolean("StartAtAppStart", false)){
					editor.putBoolean("StartAtAppStart", false);
				} else {
					editor.putBoolean("StartAtAppStart", true);
				}
				editor.commit();
				autoAppStart.setText(settings.getBoolean("StartAtAppStart", false) ? R.string.no_start_app_start_label : R.string.start_app_start_label);
			}
		});
        
        autoPhoneBoot = (Button) findViewById(R.id.phoneBoot);
        autoPhoneBoot.setText(settings.getBoolean("StartAtPhoneBoot", false) ? R.string.no_start_phone_boot_label : R.string.start_phone_boot_label);
        autoPhoneBoot.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (settings.getBoolean("StartAtPhoneBoot", false)){
					editor.putBoolean("StartAtPhoneBoot", false);
				} else {
					editor.putBoolean("StartAtPhoneBoot", true);
				}
				editor.commit();
				autoPhoneBoot.setText(settings.getBoolean("StartAtPhoneBoot", false) ? R.string.no_start_phone_boot_label : R.string.start_phone_boot_label);
			}
		});
        
    }
    
}