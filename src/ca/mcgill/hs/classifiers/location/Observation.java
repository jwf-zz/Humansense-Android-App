package ca.mcgill.hs.classifiers.location;

public abstract class Observation {
	
	public abstract double distanceFrom(Observation other);
	
	public abstract double getEPS();
}
