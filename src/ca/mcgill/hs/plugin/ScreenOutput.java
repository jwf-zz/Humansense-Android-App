package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;

import android.util.Log;

public class ScreenOutput implements OutputPlugin{

	private Thread coordinator;
	private final ReadableByteChannel rbc;
	
	public ScreenOutput(ReadableByteChannel rbc){
		this.rbc = rbc;
		coordinator = createCoordinator();
	}
	
	/**
	 * Creates the thread for this plugin.
	 * @return the Thread for this plugin.
	 */
	private Thread createCoordinator(){
		return new Thread(){
			public void run(){
				Log.i("Coordinator Thread", "Thread started.");
				ByteBuffer timestamp = ByteBuffer.allocate(8);
				try {
					while (rbc.read(timestamp) >= 0){
						timestamp.flip();
						Log.v("Receptionist", new Date(timestamp.getLong()).toString());
						timestamp.clear();
					}
				} catch (IOException e) {
					Log.e("Coordinator Thread", "THREAD CRASHED - IOEXCEPTION.");
					Log.e("HSService", e.getMessage());
				}
			}
		};
	}

	/**
	 * @override
	 */
	public void startPlugin() {
		coordinator.start();
	}
	
	/**
	 * @throws IOException 
	 * @override
	 */
	public void closePlugin(){
		try {
			rbc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
