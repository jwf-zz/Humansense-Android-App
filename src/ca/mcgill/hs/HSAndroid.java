/**
 * 
 */
package ca.mcgill.hs;

import ca.mcgill.hs.serv.HSService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class HSAndroid extends Activity{
	
	private Button button;
	private TextView tv;
	private Intent i;
	
	private boolean autoStartAppStart = false;
	private boolean autoStartPhoneBoot = false;
	private long frequency = 1000;
	
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
        
        //Setup preferences
        getPrefs();
        HSService.setUpdateInterval(frequency);
        
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
    
    private void getPrefs(){
    	//get preferences
    	SharedPreferences prefs = 
    		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	autoStartAppStart = prefs.getBoolean("autoStartAtAppStart", false);
    }
    
}