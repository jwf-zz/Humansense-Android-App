/**
 * 
 */
package ca.mcgill.hs;

import java.util.Date;
import java.util.Timer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
		switch (v.getId()){
		
		case R.id.startButton: //START BUTTON CASE
			break;
			
		case R.id.stopButton: //STOP BUTTON CASE
			break;
			
		}
	}
    
}