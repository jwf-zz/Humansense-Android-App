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
	
	/**
	 * Returns the list of output plugins this input plugin will want to write data to.
	 * @return the list of output plugins this input plugin will want to write data to.
	 */
	public Class[] getOutputClassList();

}
