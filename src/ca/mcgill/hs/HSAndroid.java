/**
 * 
 */
package ca.mcgill.hs;

import ca.mcgill.hs.serv.HSService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class HSAndroid extends Activity{
	
	private Button button;
	private TextView tv;
	private Intent i;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tv = (TextView) findViewById(R.id.counterText);
        
        //Intent
        i = new Intent(this, HSService.class);
        
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
    
}