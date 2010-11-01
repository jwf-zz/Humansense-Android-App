/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class MotionStateClusterer {
	// Contains the timestamp for the observation as well as the index in the
	// distance matrix for that observation.
	private static final class Tuple {
		public final double timestamp;
		public final int index;

		public Tuple(final double timestamp, final int index) {
			this.timestamp = timestamp;
			this.index = index;
		}
	}

	private static final int RESET_MOVEMENT_STATE_TIME_IN_SECONDS = 10;

	private BufferedWriter outputLog = null;

	private static final String RESET_MOVEMENT_STATE_TIMER_NAME = "reset movement state timer";

	private static final String TAG = "MotionStateClusterer";

	private final SignificantLocationClusterer slClusterer;
	private final LocationSet locations;
	private Timer resetMovementTimer;
	private long currentCluster = -1;

	// Maintain a queue of the Tuples for the observations that are
	// currently being clustered.
	private final LinkedList<Tuple> pool = new LinkedList<Tuple>();

	// Window length, in seconds.
	private final boolean TIME_BASED_WINDOW;

	private final int WINDOW_LENGTH;

	// Delta from the paper. This value represents the percentage of the points
	// in the pool that must neighbours of a point for it to be considered to be
	// part of a cluster
	private final float DELTA;

	// Maintain a lookup table between timestamps and observations, which
	// are sets of measurements taken at an instance in time.
	private final HashMap<Double, Observation> observations;

	private final double[][] distMatrix;

	// True if the previous window was labelled as stationary.
	private boolean previouslyMoving = true;
	private boolean currentlyMoving = false;
	private long mostRecentClusterId = -1;

	// True if the current window was labelled as stationary.
	// private boolean curStationaryStatus = false;

	private final SimpleDateFormat dfm = new SimpleDateFormat("HH:mm:ss");

	private int timerDelay = 1000 * RESET_MOVEMENT_STATE_TIME_IN_SECONDS;

	public MotionStateClusterer(final LocationSet locations) {
		TIME_BASED_WINDOW = locations.usesTimeBasedWindow();
		WINDOW_LENGTH = locations.getWindowLength();
		DELTA = locations.pctOfWindowRequiredToBeStationary();

		distMatrix = new double[WINDOW_LENGTH][WINDOW_LENGTH];
		observations = new HashMap<Double, Observation>(
				(int) (WINDOW_LENGTH / 0.75f), 0.75f);

		for (int i = 0; i < WINDOW_LENGTH; i++) {
			for (int j = 0; j < WINDOW_LENGTH; j++) {
				distMatrix[i][j] = -1;
			}
		}
		slClusterer = new SignificantLocationClusterer(locations);
		this.locations = locations;
		resetMovementTimer = new Timer(RESET_MOVEMENT_STATE_TIMER_NAME);

		final Date d = new Date(System.currentTimeMillis());
		final SimpleDateFormat dfm = new SimpleDateFormat("yy-MM-dd-HHmmss");
		final File f = new File("/sdcard/hsandroidapp/data/recent/"
				+ dfm.format(d) + "-clusters.log");
		try {
			outputLog = new BufferedWriter(new FileWriter(f));
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	// Adds a new observation to the pool, does some clustering, and then
	// returns the statuses of each point in the pool.
	public void addObservation(final double timestamp,
			final Observation observation) {

		// Delete observation outside MAX_TIME window.
		deleteOldObservations(timestamp);
		final int index = getAvailableIndex();

		// Update distance matrix
		for (final Tuple tuple : pool) {
			distMatrix[index][tuple.index] = distMatrix[tuple.index][index] = observation
					.distanceFrom(observations.get(tuple.timestamp));
		}
		observations.put(timestamp, observation);
		pool.addLast(new Tuple(timestamp, index));

		final int pool_size = pool.size();

		/*
		 * If it's a time-based window, then don't cluster if there's only one
		 * sample in the pool, otherwise if it's not a time-based window, then
		 * don't cluster until the pool is full.
		 */
		if ((TIME_BASED_WINDOW && pool_size <= 1)
				|| (!TIME_BASED_WINDOW && pool_size < WINDOW_LENGTH)) {
			return;
		}

		cluster(observation.getEPS());
	}

	public void close() {
		try {
			if (outputLog != null) {
				Log.d(TAG, "Computing Statistics");
				final File f = new File("/sdcard/hsandroidapp/clusters.dat");
				BufferedWriter statsDmp = null;
				try {
					statsDmp = new BufferedWriter(new FileWriter(f, false));
					statsDmp.write(slClusterer.toString());
					statsDmp.flush();
				} catch (final IOException e) {
					e.printStackTrace();
				} finally {
					statsDmp.close();
				}

				outputLog.close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	protected void cluster(final double eps) {
		// Perform the clustering
		final int pool_size = pool.size();
		final boolean[] clusterStatus = new boolean[WINDOW_LENGTH];
		for (int i = 0; i < WINDOW_LENGTH; i++) {
			// Set initial cluster status to false
			clusterStatus[i] = false;
		}
		for (final Tuple tuple : pool) {
			final int i = tuple.index;

			// Not the most efficient way to do things, but not too bad
			// First we check how many neighbours are within epsilon
			int neighbours = 0;
			for (final Tuple tuple2 : pool) {
				final int j = tuple2.index;
				if (i != j && distMatrix[i][j] < eps && distMatrix[i][j] > 0.0) {
					neighbours += 1;
				}
			}

			// Then, if enough neighbours exist, set ourself and our neighbours
			// to be in a cluster
			if (neighbours >= (int) (DELTA * (double) pool_size)) {
				clusterStatus[i] = true;
				for (int j = 0; j < WINDOW_LENGTH; j++) {
					if (distMatrix[i][j] < eps && distMatrix[i][j] > 0.0) {
						clusterStatus[j] = true;
					}
				}
			}
		}
		// And finally, update the statuses
		Location location = null;
		int clusteredPoints = 0;
		int status = 0;
		double timestamp;
		int index;
		for (final Tuple tuple : pool) {
			timestamp = tuple.timestamp;
			index = tuple.index;
			if (clusterStatus[index]) {
				status -= 1; // Vote for stationarity
				clusteredPoints += 1;
				/*
				 * If we were moving on the last step and now we've stopped,
				 * create a new significant location candidate.
				 */
				if (previouslyMoving) {
					if (location == null) {
						location = locations.newLocation(timestamp);
					}
					location.addObservation(observations.get(timestamp));
				}
			} else {
				status += 1; // Vote for motion.
			}
		}
		Log.d(TAG, "Clustered " + clusteredPoints + " of " + pool_size
				+ " points.");

		if (location != null) {
			currentCluster = slClusterer.addNewLocation(location);
			currentlyMoving = false;
			Log.d(TAG,
					"WifiClusterer thinks we're stationary and in location: "
							+ currentCluster);
			if (currentCluster > 0) {
				/*
				 * The purpose of this timer is to avoid continually updating
				 * the location if the user remains stationary in a known
				 * location. We start with a timer that resets the motion state
				 * every RESET_UPDATE_STATUS_TIME_IN_SECONDS seconds, and then
				 * after an update we double the time before the next update.
				 */
				resetMovementTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						previouslyMoving = true;
					}

				}, timerDelay);
				timerDelay *= 2;
				previouslyMoving = false;
			}
		} else if (!previouslyMoving && clusteredPoints == 0) {
			/*
			 * If we were stationary, but now we are moving, then we cancel the
			 * timer that should only be running if we're stationary.
			 */
			currentCluster = -1;
			timerDelay = RESET_MOVEMENT_STATE_TIME_IN_SECONDS * 1000;
			previouslyMoving = true;
			currentlyMoving = true;
			resetMovementTimer.cancel();
			resetMovementTimer.purge();
			resetMovementTimer = new Timer(RESET_MOVEMENT_STATE_TIMER_NAME);
		} else if (clusteredPoints == 0) {
			/* User was moving previously, and is still moving */
			currentCluster = -1;
			currentlyMoving = true;
		}
		try {
			if (outputLog != null) {
				outputLog.write(dfm
						.format(new Date(System.currentTimeMillis()))
						+ "," + currentCluster + "\n");
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		// LocationStatusWidget.updateWidget(clustered_points, pool_size,
		// current_cluster, currentlyMoving);
		mostRecentClusterId = currentCluster;

	}

	// Deletes the oldest observation from the pool and returns the index
	// for that point in the distance matrix, so that it can be reused.
	private void deleteOldObservations(final double timestamp) {

		// Delete anything older than WINDOW_LENGTH seconds
		if (TIME_BASED_WINDOW) {
			while (pool.size() > 0
					&& (timestamp - pool.getFirst().timestamp > WINDOW_LENGTH || pool
							.size() >= WINDOW_LENGTH)) {
				final Tuple first = pool.getFirst();
				observations.remove(first.timestamp);
				final int idx = first.index;
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					distMatrix[i][idx] = -1;
					distMatrix[idx][i] = -1;
				}
				pool.removeFirst();
			}
		}

		// Only delete the one oldest observation (ie., fixed length sliding
		// window)
		else {
			if (!pool.isEmpty() && pool.size() >= WINDOW_LENGTH) {
				final Tuple first = pool.getFirst();
				observations.remove(first.timestamp);
				final int idx = first.index;
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					distMatrix[i][idx] = -1;
					distMatrix[idx][i] = -1;
				}
				pool.removeFirst();
			}
		}
	}

	// Returns the first available index in the distance matrix.
	public int getAvailableIndex() {
		int idx = 0;
		while (distMatrix[0][idx] >= 0) {
			idx++;
		}
		return idx;
	}

	public String getClusterStatus() {
		return slClusterer.toString();
	}

	public int getMaxTime() {
		return WINDOW_LENGTH;
	}

	public long getMostRecentClusterId() {
		return currentCluster;
	}

	public int getPoolSize() {
		return pool.size();
	}

	public boolean lastObservationWasMoving() {
		return currentlyMoving;
	}
}
