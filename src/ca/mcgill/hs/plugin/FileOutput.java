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
	
	private final HashMap<Integer, DataOutputStream> fileHandles = new HashMap<Integer, DataOutputStream>();

	@Override
	void onDataReady(Object[] data, int sourceId) {
		try {
			if (!fileHandles.containsKey(sourceId)){
				final File j = new File(Environment.getExternalStorageDirectory(), "hsandroidapp/data");
				if (!j.isDirectory()) {
					if (!j.mkdirs()) {
						Log.e("Output Dir", "ARV: Could not create output directory!");
						return;
					}
				} else {
					Log.i("Output Dir", "ARV: DIRECTORY EXISTS!");
				}
				Date d = new Date(System.currentTimeMillis());
				File fh = new File(j, getSourceName(sourceId) + "" + sourceId + "" + d.getHours() + "" + d.getMinutes() + "" + d.getSeconds()+".bin");
				if (!fh.exists()) fh.createNewFile();
				Log.i("File Output", "File to write: "+fh.getName());
				fileHandles.put(sourceId, new DataOutputStream(
						new BufferedOutputStream(new GZIPOutputStream(
								new FileOutputStream(fh), 1 * 1024 // Buffer Size
						))));
			}
			for (int i = 0; i < data.length; i++){
				fileHandles.get(sourceId).writeUTF(data[i].toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void onPluginStop(){
		for (Integer id : fileHandles.keySet()){
			try {
				fileHandles.get(id).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
