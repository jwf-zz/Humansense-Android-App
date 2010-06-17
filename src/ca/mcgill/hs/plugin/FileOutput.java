package ca.mcgill.hs.plugin;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.Date;
import java.util.HashMap;

import ca.mcgill.hs.plugin.GPSLogger.GPSLocationPacket;
import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.plugin.WifiLogger.WifiLoggerPacket;

import android.os.Environment;
import android.util.Log;

/**
 * An OutputPlugin which writes data to files.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class FileOutput extends OutputPlugin{
	
	//HashMap used for keeping file handles. There is one file associated with each input plugin connected.
	private final HashMap<String, DataOutputStream> fileHandles = new HashMap<String, DataOutputStream>();
	
	//File Extensions to be added at the end of each file.
	private final String WIFI_EXT = "-wifiloc.log";
	private final String GPS_EXT = "-gpsloc.log";
	private final String SENS_EXT = "-raw.log";
	private final String DEF_EXT = ".log";
	
	/**
	 * Closes all files currently open.
	 * 
	 * @Override
	 */
	protected void onPluginStop(){
		for (String id : fileHandles.keySet()){
			try {
				fileHandles.get(id).close();
			} catch (IOException e) {
				Log.e("FileOutput", "Caught IOException");
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method gets called whenever an InputPlugin registered to FileOutput has data available
	 * to output. This method creates a file handle (if it doesn't exist already) for the InputPlugin
	 * the received DataPacket comes from. This method calls the appropriate version of dataParse based
	 * on the DataPacket type.
	 * 
	 * @param dp the DataPacket recieved.
	 * 
	 * @Override
	 */
	synchronized void onDataReceived(DataPacket dp) {
		String id = dp.getInputPluginName();
		
		try {
			if (!fileHandles.containsKey(id)){
				final File j = new File(Environment.getExternalStorageDirectory(), "hsandroidapp/data");
				if (!j.isDirectory()) {
					if (!j.mkdirs()) {
						Log.e("Output Dir", "Could not create output directory!");
						return;
					}
				}
				//Generate file name based on the plugin it came from and the current time.
				Date d = new Date(System.currentTimeMillis());
				File fh = new File(j, id + d.getHours() + "-" + d.getMinutes() + "-" + d.getSeconds()+getFileExtension(dp));
				if (!fh.exists()) fh.createNewFile();
				Log.i("File Output", "File to write: "+fh.getName());
				fileHandles.put(id, new DataOutputStream(
						new BufferedOutputStream(new GZIPOutputStream(
								new FileOutputStream(fh), 2 * 1024 // Buffer Size
						))));
			}
			
			//Choose correct dataParse method based on the format of the data received.
			DataOutputStream dos = fileHandles.get(id);
			if (dp.getClass() == WifiLoggerPacket.class){
				dataParse((WifiLoggerPacket) dp, dos);
			} else if (dp.getClass() == GPSLocationPacket.class) {
				dataParse((GPSLocationPacket) dp, dos);
			} else if (dp.getClass() == SensorLoggerPacket.class){
				dataParse((SensorLoggerPacket) dp, dos);
			}
			
		} catch (IOException e) {
			Log.e("FileOutput", "Caught IOException");
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses and writes given WifiLoggerPacket to given DataOutputStream.
	 * @param wlp the WifiLoggerPacket to parse and write out.
	 * @param dos the DataOutputStream to write to.
	 */
	private void dataParse(WifiLoggerPacket wlp, DataOutputStream dos){
		try {
			dos.writeInt(wlp.neighbors);
			dos.writeLong(wlp.timestamp);
			for (int i = wlp.neighbors - 1; i >= 0; i--){
				dos.writeInt(wlp.levels[i]);
				dos.writeUTF(wlp.SSIDs[i]);
				dos.writeUTF(wlp.BSSIDs[i]);
			}
		} catch (IOException e) {
			Log.e("FileOutput", "Caught IOException (WifiLoggerPacket parsing)");
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses and writes given SensorLoggerPacket to given DataOutputStream.
	 * @param wlp the WifiLoggerPacket to parse and write out.
	 * @param dos the DataOutputStream to write to.
	 */
	private void dataParse(SensorLoggerPacket slp, DataOutputStream dos){
		try {
			dos.writeLong(slp.timestamp);
			dos.writeFloat(slp.x);
			dos.writeFloat(slp.y);
			dos.writeFloat(slp.z);
			dos.writeFloat(slp.m);
			dos.writeFloat(slp.temperature);
			for (float f : slp.magfield) dos.writeFloat(f);
			for (float f : slp.orientation) dos.writeFloat(f);
		} catch (IOException e) {
			Log.e("FileOutput", "Caught IOException (WifiLoggerPacket parsing)");
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses and writes given GPSLoggerPacket to given DataOutputStream.
	 * @param gpslp the GPSLoggerPacket to parse and write out.
	 * @param dos the DataOutputStream to write to.
	 */
	private void dataParse(GPSLocationPacket gpslp, DataOutputStream dos){
		try {
			dos.writeLong(gpslp.time);
			dos.writeFloat(gpslp.accuracy);
			dos.writeFloat(gpslp.bearing);
			dos.writeFloat(gpslp.speed);
			dos.writeDouble(gpslp.altitude);
			dos.writeDouble(gpslp.latitude);
			dos.writeDouble(gpslp.longitude);
		} catch (IOException e) {
			Log.e("FileOutput", "Caught IOException (GPSLocationPacket parsing)");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the String corresponding to the file extension (of the DataPacket) that should be added to the 
	 * name of the file currently being created.
	 * @param dp the given DataPacket
	 * @return the String representing the extension to add to the filename.
	 */
	private String getFileExtension(DataPacket dp){
		if (dp.getClass() == WifiLoggerPacket.class){
			return WIFI_EXT;
		} else if (dp.getClass() == GPSLocationPacket.class) {
			return GPS_EXT;
		} else if (dp.getClass() == SensorLoggerPacket.class){
			return SENS_EXT;
		} else {
			return DEF_EXT;
		}
	}

}
