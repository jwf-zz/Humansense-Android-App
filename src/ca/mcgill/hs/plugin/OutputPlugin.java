package ca.mcgill.hs.plugin;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface OutputPlugin {
	
	/**
	 * Starts a thread the plugin will run.
	 */
	public void startPlugin();
	
	/**
	 * Stops the thread the plugin ran.
	 */
	public void stopPlugin();

}
