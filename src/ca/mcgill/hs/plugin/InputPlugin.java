package ca.mcgill.hs.plugin;

/**
 * A simple interface detailing the behaviour of data collecting plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface InputPlugin {
	
	/**
	 * Starts a thread the plugin will run.
	 */
	public void startPlugin();
	
	/**
	 * Stops the thread the plugin ran.
	 */
	public void stopPlugin();

}
