package ca.mcgill.hs.plugin;

import java.io.Serializable;

public interface DataPacket extends Serializable {
	public String getInputPluginName();
}
