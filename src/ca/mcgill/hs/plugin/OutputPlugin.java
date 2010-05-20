package ca.mcgill.hs.plugin;

import java.nio.channels.ReadableByteChannel;

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
	
	/**
	 * Connects the plugin to the specified ReadableByteChannel.
	 */
	public void connect(ReadableByteChannel rbc);

}
