package ca.mcgill.hs.serv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ca.mcgill.hs.plugin.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.wifi.WifiManager;
import android.os.IBinder;

public class HSService extends Service{
	
	private static boolean isRunning;
	final private LinkedList<InputPlugin> inputPluginList = new LinkedList<InputPlugin>();
	final private LinkedList<OutputPlugin> outputPluginList = new LinkedList<OutputPlugin>();
	
	public static final int STRING = 0x1;
	public static final int INT = 0x2;
	public static final int FLOAT = 0x3;
	public static final int DOUBLE = 0x4;
	public static final int LONG = 0x5;
	public static final int SHORT = 0x6;
	public static final int BYTE = 0x7;
	public static final int CHAR = 0x8;
	public static final int BOOLEAN = 0x9;
	public static final int ARRAY = 0xA;
	
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
	 * Called when the service is stopped. Also stops all plugins.
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
		
		//Setup input plugins
		try {
			for (InputPlugin input : inputPluginList){
				input.generateCallStack(getInputInfo(input.getClass().getSimpleName()));
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
		outputPluginList.add(new FileOutput());
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
				input.connect(dos);
				output.connect(dis, getInputInfo(input.getClass().getSimpleName()));
			}
		}
	}
	
	/**
	 * Generates a linked list of method calls appropriate to an input plugin. The list is to be passed to an output plugin
	 * to inform it of the variable types it should be reading from its input stream.
	 * @param inputClassName the Class name of the input plugin whose XML meta file we are parsing.
	 * @return a LinkedList of Objects containing the InputPlugin Class name, data format types, data variable names, and Method calls.
	 */
	private LinkedList<Object> getInputInfo(String inputClassName) throws XmlPullParserException, IOException{
		LinkedList<Object> result = new LinkedList<Object>();
		int identifier = getResources().getIdentifier("ca.mcgill.hs:xml/" + inputClassName.toLowerCase(), null, null);
		XmlResourceParser xrp = getResources().getXml(identifier);
		Method m;
		result.add(inputClassName);
		
		xrp.next();
		int event = xrp.getEventType();
		while (event != XmlPullParser.END_DOCUMENT){
			String name = xrp.getName();
			if (name != null && name.equals("schema")){
				xrp.next();
				event = xrp.getEventType();
				while (!(event == XmlPullParser.END_TAG && xrp.getName().equals("schema"))){
					int format = getFormatCode(xrp.getAttributeValue(0));
					xrp.next();
					String varName = xrp.getText();		
					m = generateReadMethod(format);
					result.add(format);
					result.add(varName);
					result.add(m);
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
	
	
	public static int getFormatCode(String format){
		if (format.equals("String")){
			return STRING;
		}
		else if (format.equals("int")){
			return INT;
		}
		else if (format.equals("float")){
			return FLOAT;
		}
		else if (format.equals("double")){
			return DOUBLE;
		}
		else if (format.equals("long")){
			return LONG;
		}
		else if (format.equals("byte")){
			return BYTE;
		}
		else if (format.equals("short")){
			return SHORT;
		}
		else if (format.equals("char")){
			return CHAR;
		}
		else if (format.equals("boolean")){
			return BOOLEAN;
		} else {
			return 0xF;	//0xF for failure to find correct code
		}
	}
	
	/**
	 * Generates a read____() Method appropriate to the type of data being read. This Method will be called
	 * on the DataInputStream by all OutputPlugins connected to the InputPlugin that is broadcasting the data.
	 * @param format a String representing the data format. e.g. long, int, byte, etc.
	 * @return a read____() Method appropriate for reading the data type specified by format.
	 */
	private Method generateReadMethod(int format){
		Method result = null;
		
		try {
			switch (format){
			case STRING:
				result = DataInputStream.class.getMethod("readUTF");
				break;
			case INT:
				result = DataInputStream.class.getMethod("readInt");
				break;
			case FLOAT:
				result = DataInputStream.class.getMethod("readFloat");
				break;
			case DOUBLE:
				result = DataInputStream.class.getMethod("readDouble");
				break;
			case LONG:
				result = DataInputStream.class.getMethod("readLong");
				break;
			case SHORT:
				result = DataInputStream.class.getMethod("readShort");
				break;
			case BYTE:
				result = DataInputStream.class.getMethod("readByte");
				break;
			case CHAR:
				result = DataInputStream.class.getMethod("readChar");
				break;
			case BOOLEAN:
				result = DataInputStream.class.getMethod("readBoolean");
				break;
			case ARRAY:
				result = DataInputStream.class.getMethod("readInt");
				break;
			default:
				throw new NoSuchMethodException();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
}
