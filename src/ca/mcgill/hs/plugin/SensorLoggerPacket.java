package ca.mcgill.hs.plugin;

public class SensorLoggerPacket implements DataPacket{
	
	private static final long serialVersionUID = -3517773425376067968L;
	
	final long timestamp;
	final float x;
	final float y;
	final float z;
	final float m;
	final float temperature;
	final float[] magfield;
	final float[] orientation;
	
	public SensorLoggerPacket(long timestamp, float x, float y, float z, float m, float temperature,
			final float[] magfield, final float[] orientation){
		this.timestamp = timestamp;
		this.x = x;
		this.y = y;
		this.z = z;
		this.m = m;
		this.temperature = temperature;
		this.magfield = magfield;
		this.orientation = orientation;
	}
	
	@Override
	public String getInputPluginName() {
		return "SensorLogger";
	}

}
