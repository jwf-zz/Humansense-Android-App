/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

/**
 * A simple interface describing the behaviour of plugins, both input and
 * output. Only two classes implement this, the abstract InputPlugin and
 * OutputPlugin classes. Plugins should extend these classes instead of
 * implementing this interface.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public interface Plugin {
	/**
	 * Returns the status of the plugin.
	 * 
	 * @return True if the plugin is enabled or false otherwise
	 */
	public abstract boolean isEnabled();

	/**
	 * Starts the plugin.
	 */
	public abstract void startPlugin();

	/**
	 * Stops the plugin.
	 */
	public abstract void stopPlugin();
}
