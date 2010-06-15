package ca.mcgill.hs.plugin;

import java.util.LinkedList;

import android.content.Context;
import android.preference.Preference;
import android.util.Log;

/**
 * Abstract class to be extended by all OutputPlugins. Provides an interface for using OutputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin, Runnable {
	
	private final LinkedList<DataPacket> dpList = new LinkedList<DataPacket>();
	
	public final void onDataReady(DataPacket dp){ dpList.addLast(dp); }
	
	public void run(){
		while (!dpList.isEmpty()){
			DataPacket dp = dpList.removeFirst();
			onDataReceived(dp);
		}
	}
							
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
	abstract void onDataReceived(DataPacket dp);
	
	public static Preference[] getPreferences(Context c){return null;}
	public static boolean hasPreferences(){return false;}

}
