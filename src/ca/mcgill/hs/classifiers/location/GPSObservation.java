package ca.mcgill.hs.classifiers.location;

public class GPSObservation extends Observation {

	protected final double timestamp;
	protected final int accuracy;
	protected final double latitude, longitude;
	//protected final float speed;
	
	public GPSObservation(double timestamp, int accuracy, double latitude, 
			double longitude) { //, float speed) {
		this.timestamp = timestamp;
		this.accuracy = accuracy;
		this.latitude = latitude;
		this.longitude = longitude;
		//this.speed = speed;
	}
	
	@Override
	public double distanceFrom(Observation other) {
		
		// compute distance
		GPSObservation o = (GPSObservation)other;
		return Math.sqrt((latitude-o.latitude)*(latitude-o.latitude)+(longitude-o.longitude)*(longitude-o.longitude));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("GPS Observation:\n");
		sb.append("\tLongitude: " + longitude + "\n");
		sb.append("\tLatitude: " + latitude + "\n");
		sb.append("\tAccuracy: " + accuracy);
		return sb.toString();
	}

	@Override
	public double getEPS() {
		// TODO Auto-generated method stub
		return 1E-5;
	}
}
