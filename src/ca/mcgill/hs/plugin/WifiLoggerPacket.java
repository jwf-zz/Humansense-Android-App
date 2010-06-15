package ca.mcgill.hs.plugin;

/**
 * A DataPacket class representing WifiLogger DataPackets.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class WifiLoggerPacket implements DataPacket{
	
	public final int neighbors;
	public final long timestamp;
	public final int[] levels;
	public final String[] SSIDs;
	public final String[] BSSIDs;
	
	public WifiLoggerPacket(int neighbors, long timestamp, int[] level, String[] SSID, String[] BSSID){
		this.neighbors = neighbors;
		this.timestamp = timestamp;
		this.levels = level;
		this.SSIDs = SSID;
		this.BSSIDs = BSSID;
	}

	@Override
	public String getInputPluginName() {
		return "WifiLogger";
	}
}
