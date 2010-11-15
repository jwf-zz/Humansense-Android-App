/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import android.content.Context;

public class WifiClusterer {
	private final WifiLocationSet locations;
	private final MotionStateClusterer pool;

	public WifiClusterer(final Context context) {
		locations = new WifiLocationSet(context);
		pool = new MotionStateClusterer(locations, context);
	}

	/**
	 * Close must be called or we leave the database in a bad state.
	 */
	public void close() {
		pool.close();
		locations.close();
	}

	// Returns a list the same length as timestamps that contains the votes
	// for each event. A positive value indicates motion, while a negative value
	// indicates stationarity.
	public void cluster(final WifiObservation observation) {
		pool.addObservation(observation.getTimeStamp(), observation);
	}

	public long getCurrentCluster() {
		return pool.getMostRecentClusterId();
	}

	public boolean isMoving() {
		return pool.lastObservationWasMoving();
	}
}
