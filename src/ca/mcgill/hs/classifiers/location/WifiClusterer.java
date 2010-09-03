package ca.mcgill.hs.classifiers.location;

import java.io.File;

public class WifiClusterer {
	private final WifiLocationSet locations;
	private final MotionStateClusterer pool;

	public WifiClusterer(final File dbFile) {
		locations = new WifiLocationSet(dbFile);
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
	public void cluster(final double timestamp,
			final WifiObservation observation) {
		pool.addObservation(timestamp, observation);
	}
}
