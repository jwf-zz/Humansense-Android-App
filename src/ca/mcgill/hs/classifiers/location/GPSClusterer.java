/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.File;

public class GPSClusterer {
	private final GPSLocationSet locations;
	private final MotionStateClusterer pool;

	public GPSClusterer(final File dbFile) {
		locations = new GPSLocationSet(dbFile);
		pool = new MotionStateClusterer(locations);
	}

	/**
	 * Close must be called or we leave the database in a bad state.
	 */
	public void close() {
		locations.close();
	}

	// Returns a list the same length as timestamps that contains the votes
	// for each event. A positive value indicates motion, while a negative value
	// indicates stationarity.
	public void cluster(final double timestamp, final GPSObservation observation) {
		pool.addObservation(timestamp, observation);
	}
}
