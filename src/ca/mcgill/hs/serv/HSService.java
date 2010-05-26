package ca.mcgill.hs.serv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ca.mcgill.hs.plugin.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

public class HSService extends Service{
	
	private static boolean isRunning;
	final private LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	final private LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	
	/**
	 * Returns a boolean indicating if the service is running or not.
	 * @return true if the service is running and false otherwise.
	 */
	public static boolean isRunning(){
		return isRunning;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Called when the service is started. Creates the service.
	 */
	public void onCreate(){
		super.onCreate();
	}
	
	/**
	 * Called when the service is stopped. Stops the service.
	 */
	public void onDestroy(){
		super.onDestroy();
		
		for (InputPlugin plugin : inputPluginList) plugin.stopPlugin();
		for (OutputPlugin plugin : outputPluginList) plugin.stopPlugin();
		
		isRunning = false;
	}
	
	/**
	 * Called automatically when onCreate() is called. Initialises the service and associated plug-ins and starts the service.
	 */
	public void onStart(Intent intent, int startId){
		if (isRunning)return;
		super.onStart(intent, startId);
		
		//Instantiate input plugins.
		addInputPlugins();
		
		//Instantiate output plugins
		addOutputPlugins();
		
		try {
			createConnections();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
				
		//Start input plugins.
		for (InputPlugin plugin: inputPluginList) plugin.startPlugin();
		
		//Start output plugins.
		for (OutputPlugin plugin : outputPluginList) plugin.startPlugin();
		
		//Connect inout and output plugins.
				
		isRunning = true;
		
		//Update button
		ca.mcgill.hs.HSAndroid.updateButton();
	}
	
	/**
	 * Populates the list of input plugins.
	 */
	private void addInputPlugins(){
		inputPluginList.add(new WifiLogger((WifiManager)getSystemService(Context.WIFI_SERVICE),getBaseContext()));
	}
	
	/**
	 * Populates the list of output plugins.
	 */
	private void addOutputPlugins(){
		outputPluginList.add(new ScreenOutput());
	}
	
	/**
	 * Creates the connections betweeen the input and output plugins.
	 * @throws XmlPullParserException 
	 */
	private void createConnections() throws IOException, XmlPullParserException{
		for (InputPlugin input : inputPluginList){
			for (OutputPlugin output : outputPluginList){
				Pipe p = Pipe.open();
				DataOutputStream dos = new DataOutputStream(Channels.newOutputStream(p.sink()));
				DataInputStream dis = new DataInputStream(Channels.newInputStream(p.source()));
				//Log.i("Resource test", ""+getResources().getIdentifier("ca.mcgill.hs:xml/wifilogger", null, null));			
				input.connect(dos);
				output.connect(dis, setupConnectionInfo(input.getClass().getSimpleName().toLowerCase()));
			}
		}
	}
	
	/**
	 * Generates a linked list of method calls appropriate to an input plugin. The list is to be passed to an output plugin
	 * to inform it of the variable types it should be reading from its input stream.
	 * @param inputClassName
	 * @return
	 */
	private LinkedList<Object> setupConnectionInfo(String inputClassName) throws XmlPullParserException, IOException{
		LinkedList<Object> result = new LinkedList<Object>();
		int identifier = getResources().getIdentifier("ca.mcgill.hs:xml/" + inputClassName, null, null);
		XmlResourceParser xrp = getResources().getXml(identifier);
		Method m;
		
		xrp.next();
		int event = xrp.getEventType();
		while (event != XmlPullParser.END_DOCUMENT){
			String name = xrp.getName();
			if (name != null && name.equals("schema")){
				xrp.next();
				event = xrp.getEventType();
				while (!(event == XmlPullParser.END_TAG && xrp.getName().equals("schema"))){
					String format = xrp.getAttributeValue(0);
					xrp.next();
					String varName = xrp.getText();		
					generateReadMethod(format);
					xrp.next();
					xrp.next();
					event = xrp.getEventType();
				}
			}
			xrp.next();
			event = xrp.getEventType();
		}
		
		return result;
	}
	
	private Method generateReadMethod(String format){
		Method result = null;
		
		try {
			if (format.equals("String")){
				result = DataInputStream.class.getMethod("readUTF", null);
			}
			else if (format.equals("int")){
				result = DataInputStream.class.getMethod("readInt", null);
			}
			else if (format.equals("float")){
				result = DataInputStream.class.getMethod("readFloat", null);
			}
			else if (format.equals("double")){
				result = DataInputStream.class.getMethod("readDouble", null);
			}
			else if (format.equals("long")){
				result = DataInputStream.class.getMethod("readLong", null);
			}
			else if (format.equals("byte")){
				result = DataInputStream.class.getMethod("readByte", null);
			}
			else if (format.equals("short")){
				result = DataInputStream.class.getMethod("readShort", null);
			}
			else if (format.equals("char")){
				result = DataInputStream.class.getMethod("readChar", null);
			}
			else if (format.equals("boolean")){
				result = DataInputStream.class.getMethod("readBoolean", null);
			} else {
				throw new NoSuchMethodException();
			}
		} catch (SecurityException e) {
			Log.e("METHOD", "SECURITY EXCEPTION, BIOTCH");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Log.e("METHOD", "NO SUCH METHOD RAAAGH");
			e.printStackTrace();
		}
		
		return result;
	}
	
}
