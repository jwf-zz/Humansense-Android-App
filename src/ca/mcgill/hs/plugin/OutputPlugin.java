package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;

import android.util.Log;

/**
 * Abstract class to be extended by all OutputPlugins. Provides an interface for using OutputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin {
	
	//Linked List of data input streams that each thread will read from.
	private final LinkedList<ObjectInputStream> oisList = new LinkedList<ObjectInputStream>();
	private final LinkedList<Thread> threads = new LinkedList<Thread>();
				
	//Boolean of the current state of the plugin. If true, plugin is currently running.
	private boolean running = false;
	
	/**
	 * Adds the given ObjectInputStream to the plugin's list of ObjectInputStreams.
	 * @param ois the ObjectInputStream to add.
	 */
	public final void connect(ObjectInputStream ois){
		oisList.add(ois);
	}
	
	/**
	 * Starts a thread for each DataInputStream this OutputPlugin will listen to.
	 */
	public final void startPlugin() {
		int index = 0;
		//For each ObjectInputStream this OutputPlugin will listen to, start a new thread and enumerate it.
		for (final ObjectInputStream ois : oisList){
			final int i = index;
			Thread t = new Thread(){
				private final ObjectInputStream stream = ois;
				private final int identifier = i;
				public void run(){
					Log.i(OutputPlugin.class.getSimpleName(), "Thread Started!");
					//Continually listen for data while the thread runs.
					while (running){
						try {
							DataPacket dp = (DataPacket) stream.readObject();
							if (dp != null){
								onDataReady(dp, identifier);
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			};
			threads.add(t);
			index++;
		}
		running = true;
		for (Thread t : threads) t.start();
		onPluginStart();
	}
		
	/**
	 * Stops this plugin. All threads are also halted.
	 */
	public final void stopPlugin(){
		running = false;
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

}
