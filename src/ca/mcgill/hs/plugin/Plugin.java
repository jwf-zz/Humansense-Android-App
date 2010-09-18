package ca.mcgill.hs.plugin;


/**
 * A simple interface describing the behaviour of plugins, both input and
 * output. Only two classes implement this, the abstract InputPlugin and
 * OutputPlugin classes. Plugins should extend these classes instead of
 * implementing this interface.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public interface Plugin {
	/**
	 * Sets up the plugin in order for it to run.
	 */
	public abstract void startPlugin();

	/**
	 * Cleans up once the plugin execution is halted.
	 */
	public abstract void stopPlugin();

}
