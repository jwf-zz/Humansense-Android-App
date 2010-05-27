package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import android.util.Log;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin {
	
	private final LinkedList<DataInputStream> disList = new LinkedList<DataInputStream>();
	private final ArrayList<LinkedList<String>> formatListList = new ArrayList<LinkedList<String>>();
	private final ArrayList<LinkedList<String>> nameListList = new ArrayList<LinkedList<String>>();
	private final ArrayList<LinkedList<Method>> methodListList = new ArrayList<LinkedList<Method>>();
	private final ArrayList<String> inputPluginList = new ArrayList<String>();
	private boolean running = false;
	
	public void connect(DataInputStream dis, LinkedList<Object> connectionInfo){
		disList.add(dis);
		
		LinkedList<String> formatList = new LinkedList<String>();
		LinkedList<String> nameList = new LinkedList<String>();
		LinkedList<Method> methodList = new LinkedList<Method>();
		
		if (!connectionInfo.isEmpty()){
			inputPluginList.add((String) connectionInfo.remove());
		}
		
		while (!connectionInfo.isEmpty()){
			formatList.add((String) connectionInfo.remove());
			nameList.add((String) connectionInfo.remove());
			methodList.add((Method) connectionInfo.remove());
		}
		
		formatListList.add(formatList);
		nameListList.add(nameList);
		methodListList.add(methodList);
	}
	
	/**
	 * Starts the appropriate threads and launches the plugin.
	 */
	public void startPlugin() {
		int index = 0;
		for (final DataInputStream dis : disList){
			final int i = index;
			Thread t = new Thread(){
				private final DataInputStream stream = dis;
				private final int identifier = i;
				private final LinkedList<Method> methodCalls = methodListList.get(identifier);
				public void run(){
					Log.i(getClass().getSimpleName(), "Thread Started!");
					onStart();
					while (running){
						onDataReady(read(stream, methodCalls), identifier);
					}
				}
			};
			t.start();
			index++;
		}
		running = true;
	}
	
	private Object[] read(DataInputStream dis, LinkedList<Method> methodCalls){
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
	 * Stops this plugin's threads appropriate threads.
	 */
	public void stopPlugin(){
		running = false;
		onStop();
	}
	
	protected String getSourceName(int sourceId){
		return inputPluginList.get(sourceId);
	}
	
	protected LinkedList<String> getDataFormat(int sourceId){
		return formatListList.get(sourceId);
	}
	
	protected LinkedList<String> getDataNames(int sourceId){
		return nameListList.get(sourceId);
	}
	
	protected void onStart(){}
	
	protected void onStop(){}
	
	abstract void onDataReady(Object[] data, int sourceId);

}
