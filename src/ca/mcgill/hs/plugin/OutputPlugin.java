package ca.mcgill.hs.plugin;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface OutputPlugin {
	
	/**
	 * Receives one byte of data.
	 */
	public void receiveByte(byte data);
	
	/**
	 * Closes the output plugin and saves data.
	 */
	public void closePlugin();

}
