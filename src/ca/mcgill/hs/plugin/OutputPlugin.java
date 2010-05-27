package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import android.util.Log;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin {
	
	//Linked List of data input streams that each thread will read from.
	private final LinkedList<DataInputStream> disList = new LinkedList<DataInputStream>();
	
	//ArrayLists of Linked Lists that hold the data formats, data names and method invocations needed to read
	//these data from the InputStreams that connect to this OutputPlugin.
	private final ArrayList<LinkedList<String>> formatListList = new ArrayList<LinkedList<String>>();
	private final ArrayList<LinkedList<String>> nameListList = new ArrayList<LinkedList<String>>();
	private final ArrayList<LinkedList<Method>> methodListList = new ArrayList<LinkedList<Method>>();
	
	//ArrayList of names of the plugins attached to the OutputPlugin.
	private final ArrayList<String> inputPluginList = new ArrayList<String>();
	
	//Boolean of the current state of the plugin. If true, plugin is currently running.
	private boolean running = false;
	
	/**
	 * Connects the OutputPlugin to a DataInputStream from which it will read. Each OutputPlugin can be connected
	 * multiple times to different DataInputStreams.
	 * @param dis a DataInputStream that this plugin will listen to.
	 * @param connectionInfo a LinkedList of Objects of various types generated from each InputPlugin's XML meta file.
	 */
	public final void connect(DataInputStream dis, LinkedList<Object> connectionInfo){
		//Add this DataInputStream to the plugin's list of DataInputStreams.
		disList.add(dis);
		
		/*Linked Lists containing info about the type of data this OutputPlugin will listen to.
		Data formats, variable names and the list of Methods that must be invoked in order to read these
		exact types are stored.*/
		LinkedList<String> formatList = new LinkedList<String>();
		LinkedList<String> nameList = new LinkedList<String>();
		LinkedList<Method> methodList = new LinkedList<Method>();
		
		/*The head of connectionInfo is a String with the name of the associated InputPlugin. It is removed
		and added to the inputPluginList of this OutputPlugin.*/
		if (!connectionInfo.isEmpty()){
			inputPluginList.add((String) connectionInfo.remove());
		}
		
		//Fill each list with the appropriate data from the connectionInfo list.
		while (!connectionInfo.isEmpty()){
			formatList.add((String) connectionInfo.remove());
			nameList.add((String) connectionInfo.remove());
			methodList.add((Method) connectionInfo.remove());
		}
		
		//Fill each ArrayList with the Linked Lists.
		formatListList.add(formatList);
		nameListList.add(nameList);
		methodListList.add(methodList);
	}
	
	/**
	 * Starts a thread for each DataInputStream this OutputPlugin will listen to.
	 */
	public final void startPlugin() {
		int index = 0;
		//For each DataInputStream this OutputPlugin will listen to, start a new thread and enumerate it.
		for (final DataInputStream dis : disList){
			final int i = index;
			Thread t = new Thread(){
				private final DataInputStream stream = dis;
				private final int identifier = i;
				private final LinkedList<Method> methodCalls = methodListList.get(identifier);
				public void run(){
					Log.i(getClass().getSimpleName(), "Thread Started!");
					//Continually listen for data while the thread runs.
					while (running){
						onDataReady(read(stream, methodCalls), identifier);
					}
				}
			};
			t.start();
			index++;
		}
		running = true;
		onPluginStart();
	}
	
	/**
	 * Reads from the given DataInputStream by sequentially calling the methods given in the LinkedList<Method>.
	 * @param dis the DataInputStream to read from.
	 * @param methodCalls the sequence of Method calls appropriate for this thread.
	 * @return an Object array containing the data that was read from the DataInputStream.
	 */
	private final Object[] read(DataInputStream dis, LinkedList<Method> methodCalls){
		Object[] data = new Object[methodCalls.size()];
		int i = 0;
		for (Method m : methodCalls){
			try {
				data[i] = m.invoke(dis);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			i++;
		}
		return data;
	}
	
	/**
	 * Stops this plugin. All threads are also halted.
	 */
	public final void stopPlugin(){
		running = false;
		onPluginStop();
	}
	
	/**
	 * Returns the name of the InputPlugin associated with the thread indexed by this sourceId.
	 * @param sourceId the Id number of the thread whose associated InputPlugin we want.
	 * @return the name of the InputPlugin associated with the thread indexed by this sourceId.
	 */
	protected final String getSourceName(int sourceId){
		return inputPluginList.get(sourceId);
	}
	
	/**
	 * Returns the data format list of the InputPlugin associated with the thread indexed by this sourceId.
	 * @param sourceId the Id number of the thread whose associated InputPlugin data format list we want.
	 * @return the data format list of the InputPlugin associated with the thread indexed by this sourceId.
	 */
	protected final LinkedList<String> getDataFormat(int sourceId){
		return formatListList.get(sourceId);
	}
	
	/**
	 * Returns the data variable names list of the InputPlugin associated with the thread indexed by this sourceId.
	 * @param sourceId the Id number of the thread whose associated InputPlugin data format list we want.
	 * @return the data variable names list of the InputPlugin associated with the thread indexed by this sourceId.
	 */
	protected final LinkedList<String> getDataNames(int sourceId){
		return nameListList.get(sourceId);
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
	 * An abstract method implemented in each OutputPlugin. It is called whenever data is ready.
	 * This method is used by all data input threads associated with this plugin so statements
	 * must be written to filter data from different InputPlugins that will call this method.
	 * @param data an Object array of the appropriate data types broadcast by an InputPlugin.
	 * @param sourceId the Id number of the thread associated with the source InputPlugin.
	 */
	abstract void onDataReady(Object[] data, int sourceId);

}
