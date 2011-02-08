/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

public class GPSObservation extends Observation {

	protected final double timestamp;
	protected final int accuracy;
	protected final double latitude, longitude;

	// protected final float speed;

	/**
	 * Constructs a new GPSObservation with the given parameters.
	 */
	public GPSObservation(final double timestamp, final int accuracy,
			final double latitude, final double longitude) { // , float speed) {
		this.timestamp = timestamp;
		this.accuracy = accuracy;
		this.latitude = latitude;
		this.longitude = longitude;
		// this.speed = speed;
	}

	@Override
	public double distanceFrom(final Observation other) {
		/*
		 * Distance is Euclidean. Maybe better to use geodesic distances, but
		 * we're really only concerned with locations that are really close to
		 * each other.
		 */
		final GPSObservation o = (GPSObservation) other;
		return Math.sqrt((latitude - o.latitude) * (latitude - o.latitude)
				+ (longitude - o.longitude) * (longitude - o.longitude));
	}

	@Override
	public double getEPS() {
		return 1E-5;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("GPS Observation:\n");
		sb.append("\tLongitude: " + longitude + "\n");
		sb.append("\tLatitude: " + latitude + "\n");
		sb.append("\tAccuracy: " + accuracy);
		return sb.toString();
	}
}
