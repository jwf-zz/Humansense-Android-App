package ca.mcgill.hs.plugin;

import android.content.Context;
import android.preference.Preference;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for using InputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
		
	
	protected void write(DataPacket dp){
		
	}
	
	public static Preference[] getPreferences(Context c){return null;}
	
	public static boolean hasPreferences() {return false;}

}
