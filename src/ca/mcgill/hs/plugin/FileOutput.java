package ca.mcgill.hs.plugin;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.zip.GZIPOutputStream;

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
	
	private Thread fileOutputThread;
	private boolean threadRunning = false;
	private DataOutputStream dos;
	private PipedInputStream pis;
	final private String FILE_NAME = "fileOutputPluginOutput.out";
	private File file;
	
	/**
	 * Constructor.
	 */
	public FileOutput(PipedInputStream pis){
		this.pis = pis;
		
		final File j = new File(Environment.getExternalStorageDirectory(), "hs/data");
		if (!j.isDirectory()) {
			if (!j.mkdirs()) {
				Log.e("FileOutput Plugin", "Could not create output directory!");
				return;
			}
		}
		
		file = new File(j, FILE_NAME);
		
		try {
			dos = new DataOutputStream(
					new BufferedOutputStream(new GZIPOutputStream(
							new FileOutputStream(file), 1 * 1024 // Buffer Size
					)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @override
	 */
	public void startPlugin() {
		fileOutputThread = new Thread() {
			public void run() {
				while(threadRunning) {
					try {
						dos.write((byte) pis.read());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		fileOutputThread.start();
		Log.i("FileOutput", "Started thread.");
		
		threadRunning = true;
		
	}

	/**
	 * @override
	 */
	public void stopPlugin() {
		threadRunning = false;
		Log.i("FileOutput", "Thread wuz killed by DJ Werd.");
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
