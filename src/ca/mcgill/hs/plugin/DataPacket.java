package ca.mcgill.hs.plugin;

/**
 * Interface implemented by all data packet classes contained within InputPlugin implementations.
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface DataPacket {
	
	/**
	 * Returns a String representing the name of the InputPlugin that created this DataPacket.
	 * @return a String representing the name of the InputPlugin.
	 */
	public String getInputPluginName();
	public int getInputPluginId(); // For faster lookups
	
	/**
	 * Clones the DataPacket by instantiating a new DataPacket.
	 * @return the cloned DataPacket.
	 */
	public DataPacket clone();
}
