package ca.mcgill.hs.plugin;


/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to the
 * android's logcat. However, this only works for wifi data coming from the WifiLogger
 * input plugin.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class ScreenOutput implements OutputPlugin{

	private Thread coordinator;
	private boolean running = false;
	
	/**
	 * This is the basic constructor for the ScreenOutput plugin.
	 */
	public ScreenOutput(){
		coordinator = createCoordinator();
	}
	
	/**
	 * Creates the thread for this plugin. Specifies the behaviour for the information
	 * gathering.
	 * 
	 * @return the Thread for this plugin.
	 */
	private Thread createCoordinator(){
		return new Thread(){
			public void run(){
				while (running){
					
				}
			}
		};
	}

	/**
	 * Starts the appropriate threads and launches the plugin.
	 * 
	 * @override
	 */
	public void startPlugin() {
		coordinator.start();
		running = true;
	}
	
	/**
	 * Stops the appropriate threads.
	 * 
	 * @override
	 */
	public void closePlugin(){
		running = false;
	}

}
