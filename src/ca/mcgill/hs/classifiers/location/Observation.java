/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

/**
 * Contains observations, derived from sensor data. Observations form the basis
 * of {@link Location}s.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public abstract class Observation {

	/**
	 * Computes the distance between this observation and another.
	 * 
	 * @param other
	 *            The observation against which to compare this one.
	 * @return A distance value.
	 */
	public abstract double distanceFrom(Observation other);

	/**
	 * Epsilon is the maximum distance between two points for them to be
	 * considered neighbours.
	 * 
	 * @return Epsilon
	 */
	public abstract double getEPS();
}
