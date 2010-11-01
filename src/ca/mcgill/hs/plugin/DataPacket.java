/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

/**
 * Interface implemented by all data packet classes contained within InputPlugin
 * implementations.
 * 
 */
public interface DataPacket {

	/**
	 * Clones the DataPacket by instantiating a new DataPacket.
	 * 
	 * @return the cloned DataPacket.
	 */
	public DataPacket clone();

	/**
	 * Returns this DataPacket's ID to allow for fast identification of the
	 * packet type.
	 * 
	 * @return the DataPacket ID
	 */
	public int getDataPacketId();

	/**
	 * Returns a String representing the name of the InputPlugin that created
	 * this DataPacket.
	 * 
	 * @return a String representing the name of the InputPlugin.
	 */
	public String getInputPluginName();
}
