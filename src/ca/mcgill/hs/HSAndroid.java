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


public class HSAndroid extends Activity implements OnClickListener{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Buttons
        View startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        View stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);

    }

	@Override
	public void onClick(View v) {
		Intent i;
		
		switch (v.getId()){
		
		case R.id.startButton: //START BUTTON CASE
			i = new Intent(this, HSService.class);
			startService(i);
			break;
			
		case R.id.stopButton: //STOP BUTTON CASE
			i = new Intent(this, HSService.class);
			stopService(i);
			break;
			
		}
	}
    
}