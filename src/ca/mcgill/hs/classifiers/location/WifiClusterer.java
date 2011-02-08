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

	/**
	 * Constructs a new clusterer for wifi signals. Requires the context in
	 * order to access the databases for this application.
	 * 
	 * @param context
	 *            The application context.
	 */
	public WifiClusterer(final Context context) {
		locations = new WifiLocationSet(context);
		pool = new MotionStateClusterer(locations);
	}

	/**
	 * Closes the clusterer. It is important that this method be called or the
	 * database may not be properly closed.
	 */
	public void close() {
		pool.close();
		locations.close();
	}

	/**
	 * Adds a new observation to the pool, and attempts to cluster it.
	 * 
	 * @param observation
	 *            The new observation to add to the cluster pool.
	 */
	public void cluster(final WifiObservation observation) {
		pool.addObservation(observation.getTimeStamp(), observation);
	}

	/**
	 * @return The id of the most recently assigned cluster.
	 */
	public long getCurrentCluster() {
		return pool.getMostRecentClusterId();
	}

	/**
	 * @return True if the last observation was deemed moving, or False if the
	 *         last observation was deemed stationary.
	 */
	public boolean isMoving() {
		return pool.lastObservationWasMoving();
	}
}
