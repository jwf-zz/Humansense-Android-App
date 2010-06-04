package ca.mcgill.hs.plugin;

public class GPSLocationPacket implements DataPacket{

	private static final long serialVersionUID = 8914866684642852066L;
	
	final long time;
	final float accuracy;
	final float bearing;
	final float speed;
	final double altitude;
	final double latitude;
	final double longitude;
	
	public GPSLocationPacket(long time, float accuracy, float bearing, float speed, double altitude, double latitude, double longitude){
		this.time = time;
		this.accuracy = accuracy;
		this.bearing = bearing;
		this.speed = speed;
		this.altitude = altitude;
		this.latitude = latitude;
		this.longitude = longitude;
	}

}
