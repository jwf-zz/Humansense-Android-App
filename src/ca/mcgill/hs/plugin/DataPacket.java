package ca.mcgill.hs.plugin;

public interface DataPacket {
	public String getInputPluginName();
	public DataPacket clone();
}
