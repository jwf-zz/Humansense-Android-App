package ca.mcgill.hs.plugin;

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
	
	public int getNeighbors(){
		return levels.length;
	}
}
