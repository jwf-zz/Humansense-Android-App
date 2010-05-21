package ca.mcgill.hs.plugin;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface OutputPlugin {
	
	/**
	 * Starts the output plugin and saves data.
	 */
	public void startPlugin();
	
	/**
	 * Closes the output plugin and saves data.
	 */
	public void closePlugin();

}
