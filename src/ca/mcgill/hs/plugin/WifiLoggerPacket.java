package ca.mcgill.hs.plugin;

/**
 * A DataPacket class representing WifiLogger DataPackets.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLoggerPacket implements DataPacket{
	private static final long serialVersionUID = 8968453425171905992L;
	
	public final long timestamp;
	public final int[] levels;
	public final String[] SSIDs;
	public final String[] BSSIDs;
	
	public WifiLoggerPacket(long timestamp, int[] level, String[] SSID, String[] BSSID){
		this.timestamp = timestamp;
		this.levels = level;
		this.SSIDs = SSID;
		this.BSSIDs = BSSID;
	}
	
	/**
	 * Returns the size of the result set contained in this packet.
	 * @return the size of the result set.
	 */
	public int getNeighbors(){
		return levels.length;
	}

	@Override
	public String getInputPluginName() {
		return "WifiLogger";
	}
}
