package ca.mcgill.hs.plugin;

import java.nio.channels.WritableByteChannel;

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
	
	/**
	 * Connects the input plugin to the appropriate WritableByteChannel.
	 * @param wbc the WritableByteChannel to write to.
	 * @return true if the connection succeeds, false otherwise.
	 */
	public boolean connect(WritableByteChannel wbc);
	
	/**
	 * Returns the input plugin's data type code. The data type code specifies the type of
	 * data this input plugin will be reading and writing to its WritableByteChannels. Types
	 * of data include:
	 * 0 - Wifi
	 * @return the input plugin's data type code.
	 */
	public byte getTypeCode();

}
