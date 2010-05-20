package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;

import android.util.Log;

public class ScreenOutput implements OutputPlugin{

	private Thread coordinator;
	private ReadableByteChannel rbc;
	
	public ScreenOutput(){
		coordinator = createCoordinator();
	}
	
	/**
	 * Creates the thread for this plugin.
	 * @return the Thread for this plugin.
	 */
	private Thread createCoordinator(){
		return new Thread(){
			public void run(){
				if (rbc == null) return;
				else {
					Log.i("Coordinator Thread", "Thread started.");
					ByteBuffer bufferIn = ByteBuffer.allocate(8 + 4 + 50 + 4 + 50 + 8);
					try {
						while (rbc.read(bufferIn) >= 0){
						
							Log.i("Receptionist", "Sir, we have a new connection");
						
							Log.v("Receptionist", new Date(bufferIn.getLong(0)).toString());
						
							bufferIn.position(8);
							Log.v("Receptionist", "Connection level is " + bufferIn.getInt());
						
							bufferIn.position(12);
							int ssidLength = bufferIn.getInt();
						
							bufferIn.position(16);
							String s = "";
							while (ssidLength > 0){
								s = s.concat(String.valueOf(bufferIn.getChar()));
								ssidLength--;
							}
							Log.v("Receptionist", "SSID: " + s);
						
							int bssidLength = bufferIn.getInt();
							String b = "";
							while (bssidLength > 0){
								b = b.concat(String.valueOf(bufferIn.getChar()));
								bssidLength--;
							}
							Log.v("Receptionist", "BSSID: " + b);
						
						
						}
					} catch (IOException e) {
						Log.e("Coordinator Thread", "THREAD CRASHED - IOEXCEPTION.");
						Log.e("HSService", e.getMessage());
					}
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
	 * @override
	 */
	public void closePlugin(){
		try {
			rbc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @override
	 */
	public void connect(ReadableByteChannel rbc) {
		this.rbc = rbc;
	}

}
