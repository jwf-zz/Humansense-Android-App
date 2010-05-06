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
import android.view.Menu;
import android.view.MenuItem;
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
	private static final int MENU_SETTINGS = 37043704;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tv = (TextView) findViewById(R.id.counterText);
        
        //Intent
        i = new Intent(this, HSService.class);
        
        //Preference Startup
        settings = getSharedPreferences(HSANDROID_PREFS_NAME, Activity.MODE_WORLD_READABLE);
        editor = settings.edit();
        if (!settings.contains("Frequency")) editor.putInt("Frequency", 1000);
        if (!settings.contains("StartAtPhoneBoot"))editor.putBoolean("StartAtPhoneBoot", false);
        if (!settings.contains("StartAtAppStart"))editor.putBoolean("StartAtAppStart", false);
        editor.commit();
        
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
        
        //Application AutoStart
        if (settings.getBoolean("StartAtAppStart", false) && !HSService.isRunning()){
        	startService(i);
        	button.setText(R.string.stop_label);
        }
        
    }
    
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, "Settings");
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SETTINGS:
            Intent i = new Intent(getBaseContext(), ca.mcgill.hs.prefs.HSAndroidPreferences.class);
            startActivity(i);
            break;
        }
        return false;
    }
    
}