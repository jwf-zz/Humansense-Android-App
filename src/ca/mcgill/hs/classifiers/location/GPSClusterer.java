/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.File;

import android.content.Context;

//TODO: Make this work.
/**
 * Clusters GPS data based on locations in which the user spends a significant
 * amount of time. Doesn't work yet!
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class GPSClusterer {
	private final GPSLocationSet locations;
	private final MotionStateClusterer pool;

	/**
	 * Doesn't work yet.
	 * 
	 * @param dbFile
	 * @param context
	 */
	public GPSClusterer(final File dbFile, final Context context) {
		locations = new GPSLocationSet(dbFile);
		pool = new MotionStateClusterer(locations);
	}

	/**
	 * Close must be called or we leave the database in a bad state.
	 */
	public void close() {
		locations.close();
	}

	/**
	 * Adds a new observation to the cluster pool.
	 * 
	 * @param timestamp
	 *            Timestamp in milliseconds associated with this observation.
	 * @param observation
	 *            The {@link GPSObservation} to be clustered.
	 */
	public void cluster(final double timestamp, final GPSObservation observation) {
		pool.addObservation(timestamp, observation);
	}
}
