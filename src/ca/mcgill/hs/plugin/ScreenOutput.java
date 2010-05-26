package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.util.Log;


/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to the
 * android's logcat. However, this only works for wifi data coming from the WifiLogger
 * input plugin.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class ScreenOutput extends OutputPlugin{

	private boolean running = false;

	private final LinkedList<Thread> threadList = new LinkedList<Thread>();
	
	/**
	 * Starts the appropriate threads and launches the plugin.
	 * 
	 * @override
	 */
	public void startPlugin() {
		int i = 0;
		for (final DataInputStream dis : disList){
			threadList.add(new Thread(){
				public void run(){
					Log.i("Screen Output", "Thread Started!");
					while (running){
						try {
							Log.i("Screen Output", ""+new Date(dis.readLong()).toString());
							Log.i("Screen Output", "Level: "+dis.readInt());
							Log.i("Screen Output", "SSID: "+dis.readUTF());
							Log.i("Screen Output", "BSSID: "+dis.readUTF());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
			threadList.getLast().start();
			i++;
		}
		running = true;
	}
	
	/**
	 * Stops the appropriate threads.
	 * 
	 * @override
	 */
	public void stopPlugin(){
		running = false;
	}

}
