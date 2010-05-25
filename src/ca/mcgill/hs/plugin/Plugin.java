package ca.mcgill.hs.plugin;

public interface Plugin {
	
	/**
	 * Starts a thread the plugin will run.
	 */
	public void startPlugin();
	
	/**
	 * Stops the thread the plugin ran.
	 */
	public void stopPlugin();

}
