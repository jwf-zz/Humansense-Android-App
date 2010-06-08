package ca.mcgill.hs.plugin;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import android.os.Environment;
import android.util.Log;

public class FileOutput extends OutputPlugin{
	
	//HashMap used for keeping file handles. There is one file associated with each input plugin connected.
	private final HashMap<Integer, DataOutputStream> fileHandles = new HashMap<Integer, DataOutputStream>();
	
	protected void onPluginStop(){
		for (Integer id : fileHandles.keySet()){
			try {
				fileHandles.get(id).close();
			} catch (IOException e) {
				Log.e("FileOutput", "Caught IOException");
				e.printStackTrace();
			}
		}
	}

	@Override
	void onDataReady(DataPacket dp, int sourceId) {
		try {
			if (!fileHandles.containsKey(sourceId)){
				final File j = new File(Environment.getExternalStorageDirectory(), "hsandroidapp/data");
				if (!j.isDirectory()) {
					if (!j.mkdirs()) {
						Log.e("Output Dir", "Could not create output directory!");
						return;
					}
				} else {
					Log.i("Output Dir", "DIRECTORY EXISTS!");
				}
				//Generate file name based on the plugin it came from and the current time.
				Date d = new Date(System.currentTimeMillis());
				File fh = new File(j, dp.getInputPluginName() + d.getHours() + "-" + d.getMinutes() + "-" + d.getSeconds()+getFileExtension(dp));
				if (!fh.exists()) fh.createNewFile();
				Log.i("File Output", "File to write: "+fh.getName());
				fileHandles.put(sourceId, new DataOutputStream(
						new BufferedOutputStream(new GZIPOutputStream(
								new FileOutputStream(fh), 2 * 1024 // Buffer Size
						))));
			}
			
			//Choose correct dataParse method based on the format of the data received.
			DataOutputStream dos = fileHandles.get(sourceId);
			if (dp.getClass() == WifiLoggerPacket.class){
				dataParse((WifiLoggerPacket) dp, dos);
			} else if (dp.getClass() == GPSLocationPacket.class) {
				dataParse((GPSLocationPacket) dp, dos);
			}
			
		} catch (FileNotFoundException e) {
			Log.e("FileOutput", "Caught FileNotFoundException");
			e.printStackTrace();
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
	 * Parses and writes given GPSLoggerPacket to given DataOutputStream.
	 * @param gpslp
	 * @param dos
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
	
	private String getFileExtension(DataPacket dp){
		if (dp.getClass() == WifiLoggerPacket.class){
			return "-wifiloc.log";
		} else if (dp.getClass() == GPSLocationPacket.class) {
			return "-gpsloc.log";
		} else {
			return ".log";
		}
	}

}
