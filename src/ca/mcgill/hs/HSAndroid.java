/**
 * TODO: Insert licenses here.
 */
package ca.mcgill.hs;

import ca.mcgill.hs.serv.HSService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * This Activity is the entry point to the HSAndroid application. This Activity is launched
 * manually on the phone by the user, and is from where the background services can be manually
 * started and stopped, and where the preferences and settigns can be changed.
 * 
 * @author Jonathan Pitre
 * 
 */
public class HSAndroid extends Activity{
	
	private static Button button;
	private TextView tv;
	private Intent i;
	
	private boolean autoStartAppStart = false;
	
	public static final String HSANDROID_PREFS_NAME = "HSAndroidPrefs";
	private static final int MENU_SETTINGS = 37043704;
	
    /**
     * This method is called when the activity is first created. It is the entry
     * point for the application.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tv = (TextView) findViewById(R.id.counterText);
        
        //Intent
        i = new Intent(this, HSService.class);
        
        //Setup preferences
        getPrefs();
        
        //Auto App Start
        if (autoStartAppStart){
        	startService(i);
        }
        
        //Buttons
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener( new View.OnClickListener() {
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
        
    }
    
    /**
     * This method is called whenever the user wants to access the settings menu.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, "Settings").setIcon(R.drawable.options);
        return true;
    }

    /**
     * This method is used to parse the selection of options items. These items include:
     * - Preferences (settings)
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SETTINGS:
            Intent i = new Intent(getBaseContext(), ca.mcgill.hs.prefs.HSAndroidPreferences.class);
            startActivity(i);
            break;
        }
        return false;
    }
    
    /**
     * Sets up the preferences, i.e. get Activity preferences.
     */
    private void getPrefs(){
    	SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	autoStartAppStart = prefs.getBoolean("autoStartAtAppStart", false);
    }
    
    /**
     * Updates the main starting button. This is required due to the nature of Activities
     * in the Android API. In order to correctly get the state of the service to update
     * the button text, this method cannot be called from within the Activity.
     */
    public static void updateButton(){
    	if (button != null) button.setText((HSService.isRunning() ? R.string.stop_label : R.string.start_label));
    }
    
}