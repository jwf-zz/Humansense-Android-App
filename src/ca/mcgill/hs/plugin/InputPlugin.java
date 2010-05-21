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
	 * Returns the list of output plugins this input plugin will want to write data to.
	 * @return the list of output plugins this input plugin will want to write data to.
	 */
	public Class[] getOutputClassList();

}
