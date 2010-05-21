package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;

import android.util.Log;

public class ScreenOutput implements OutputPlugin{

	private Thread coordinator;
	private boolean running = false;
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
				Log.i("Coordinator Thread", "Thread started.");
				while (running){
					if (rbc == null) return;
					else {
						ByteBuffer sizeIn = ByteBuffer.allocate(4);
						try {
							rbc.read(sizeIn);
							
							sizeIn.flip();
							
							int bufferSize = sizeIn.getInt();
							
							ByteBuffer bufferIn = ByteBuffer.allocate(bufferSize);
							
							rbc.read(bufferIn);
								
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
							
						} catch (IOException e) {
							Log.e("Coordinator Thread", "THREAD CRASHED - IOEXCEPTION.");
							Log.e("HSService", e.getMessage());
						}
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
		running = true;
	}
	
	/**
	 * @override
	 */
	public void closePlugin(){
		running = false;
	}

	/**
	 * @override
	 */
	public void connect(ReadableByteChannel rbc) {
		this.rbc = rbc;
	}

}
