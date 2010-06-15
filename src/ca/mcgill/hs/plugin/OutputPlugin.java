package ca.mcgill.hs.plugin;

import android.content.Context;
import android.preference.Preference;

/**
 * Abstract class to be extended by all OutputPlugins. Provides an interface for using OutputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin {
	
	private DataPacket dp;
							
	/**
	 * Starts a thread for each DataInputStream this OutputPlugin will listen to.
	 */
	public final void startPlugin() {
		onPluginStart();
	}
		
	/**
	 * Stops this plugin. All threads are also halted.
	 */
	public final void stopPlugin(){
		onPluginStop();
	}
		
	/**
	 * Called when this OutputPlugin is started. This method is meant to be overridden.
	 */
	protected void onPluginStart(){}
	
	/**
	 * Called when this OutputPlugin is stopped. This method is meant to be overridden.
	 */
	protected void onPluginStop(){}
	
	/**
	 * Called when there is data available for this plugin.
	 * @param dp the DataPacket that this plugin is receiving.
	 * @param sourceId the ID of the input plugin that sent this DataPacket.
	 */
	abstract void onDataReady(DataPacket dp, int sourceId);
	
	public static Preference[] getPreferences(Context c){return null;}
	public static boolean hasPreferences(){return false;}

}
