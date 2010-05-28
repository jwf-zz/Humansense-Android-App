package ca.mcgill.hs.plugin;

/**
 * A simple interface describing the behaviour of plugins, both input and output. Only two classes
 * implement this, the abstract InputPlugin and OutputPlugin classes. Plugins should extend these
 * classes instead of implementing this interface.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface Plugin {
	
	/**
	 * Starts a threads the plugin will run.
	 */
	public void startPlugin();
	
	/**
	 * Stops the threads the plugin ran.
	 */
	public void stopPlugin();

}
