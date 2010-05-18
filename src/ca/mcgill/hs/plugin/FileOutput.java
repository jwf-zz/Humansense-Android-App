package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

/**
 * An incredibly simple OutputPlugin which reads the data from an input stream and writes
 * it to a file specified in FILE_NAME.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class FileOutput implements OutputPlugin{
	
	private DataOutputStream dos;
	private String buffer = "";
	final private String FILE_NAME = "fileOutputPluginOutput.out";
	private File file;
	
	/**
	 * Constructor.
	 */
	public FileOutput(){
		
		final File j = new File(Environment.getExternalStorageDirectory(), "hs/data");
		if (!j.isDirectory()) {
			if (!j.mkdirs()) {
				Log.e("FileOutput Plugin", "Could not create output directory!");
				return;
			}
		}
		
		file = new File(j, FILE_NAME);
		if (file.exists()){
			file.delete(); 
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			dos = new DataOutputStream(
							new FileOutputStream(file)
					);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @override
	 */
	public void receiveByte(byte data) {
		try {
			if (Character.isLetter((char) data) || (char) data == '.' || (char) data == ' ' || (char) data == '-'){
				buffer = buffer.concat(Character.toString((char) data));
			} else if (buffer.length() > 1) {
				dos.writeChars(buffer + '\n');
				buffer = "";
			} else {
				buffer = "";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @override
	 */
	public void closePlugin() {
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
