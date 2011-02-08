/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.util.Collection;
import java.util.LinkedList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class LocationSet {

	// Table Names
	public static final String LOCATIONS_TABLE = "locations";
	public static final String NEIGHBOURS_TABLE = "neighbours";
	public static final String LABELS_TABLE = "labels";
	public static final String CLUSTERS_TABLE = "clusters";

	public static final String SQLITE_DATE_FORMAT = "'%Y-%m-%d %H:%M:%f'";

	/**
	 * Delta from the paper. This value represents the percentage of the points
	 * in the pool that must neighbours of a point for it to be considered to be
	 * part of a cluster
	 */
	protected static final float DELTA = 0.90f;

	protected SQLiteDatabase db = null;

	/**
	 * Adds the point the set, and computes all its neighbors upon its addition.
	 * 
	 * @param point
	 *            The point to be added to the set.
	 * @return The id of the location, which may have changed if the location
	 *         gets merged with another existing location.
	 */
	public abstract long add(Location point);

	/**
	 * Adds a location to a cluster.
	 * 
	 * @param locationId
	 *            The id of the location to be added to the cluster.
	 * @param clusterId
	 *            The id of the cluster to add the location to.
	 */
	public void addToCluster(final long locationId, final long clusterId) {
		db.execSQL("REPLACE INTO " + CLUSTERS_TABLE + " VALUES (" + locationId
				+ "," + clusterId + ", NULL);");
	}

	/**
	 * Relabels a cluster with a new id. This is used when merging clusters.
	 * 
	 * @param oldId
	 *            Old cluster id.
	 * @param newId
	 *            New cluster id.
	 */
	public void changeClusterId(final long oldId, final long newId) {
		if (oldId == newId) {
			return;
		}
		DebugHelper.out.println("\tChanging cluster " + oldId + " to cluster "
				+ newId);
		db.execSQL("UPDATE " + CLUSTERS_TABLE + " SET cluster_id=" + newId
				+ " WHERE cluster_id=" + oldId + ";");
	}

	/**
	 * Returns the ids of all clusters in the database.
	 * 
	 * @return A collection of cluster ids.
	 */
	public Collection<Long> getAllClusters() {
		final Collection<Long> clusters = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT DISTINCT cluster_id FROM "
				+ CLUSTERS_TABLE + ";", null);
		try {
			while (cursor.moveToNext()) {
				clusters.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return clusters;
	}

	/**
	 * Returns the ids of all locations in the database.
	 * 
	 * @return A collection of location ids.
	 */
	public Collection<Long> getAllLocations() {
		final Collection<Long> locations = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM "
				+ LOCATIONS_TABLE + ";", null);
		try {
			while (cursor.moveToNext()) {
				locations.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return locations;
	}

	/**
	 * Gets the cluster id for a particular location id.
	 * 
	 * @param locationId
	 *            The location id.
	 * @return Cluster id, or -1 if point is not in a cluster.
	 */
	public long getClusterId(final long locationId) {
		long cluster_id = -1;
		final Cursor cursor = db.rawQuery("SELECT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id=?;", new String[] { Long
				.toString(locationId) });
		try {
			if (cursor.moveToNext()) {
				cluster_id = cursor.getLong(0);
			}
		} finally {
			cursor.close();
		}
		return cluster_id;
	}

	/**
	 * Returns a collection of all of the cluster ids that appear in collection
	 * of locations
	 * 
	 * @param locations
	 *            The set of locations to check
	 * @return The cluster ids that occur in the collection of locations.
	 */
	public Collection<Long> getClusterIds(final Collection<Long> locations) {
		final StringBuilder locationIds = new StringBuilder();
		boolean first = true;
		for (final long locationId : locations) {
			if (first) {
				locationIds.append(locationId);
				first = false;
			} else {
				locationIds.append("," + locationId);
			}
		}

		final Collection<Long> clusters = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT DISTINCT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id IN ("
				+ locationIds.toString() + ")", null);
		try {
			while (cursor.moveToNext()) {
				clusters.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return clusters;
	}

	/**
	 * Retrieves a location from the database.
	 * 
	 * @param locationId
	 *            The location id to retrieve.
	 */
	public abstract Location getLocation(long locationId);

	/**
	 * Gets all of the locations contained in the specified cluster
	 * 
	 * @param clusterId
	 *            The id of the cluster.
	 * @return A collection of location ids.
	 */
	public Collection<Long> getLocationsForCluster(final long clusterId) {
		final Collection<Long> ids = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM "
				+ CLUSTERS_TABLE + " WHERE cluster_id=?", new String[] { Long
				.toString(clusterId) });
		try {
			while (cursor.moveToNext()) {
				ids.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return ids;
	}

	/**
	 * Returns the next available cluster id.
	 * 
	 * @return An available cluster id.
	 */
	public long getNewClusterId() {
		long cluster_id = -1;
		final Cursor cursor = db.rawQuery("SELECT MAX(cluster_id) FROM "
				+ CLUSTERS_TABLE + ";", null);
		try {
			if (cursor.moveToNext()) {
				cluster_id = cursor.getLong(0) + 1;
			}
		} finally {
			cursor.close();
		}
		return cluster_id;
	}

	/**
	 * @return The window length, in samples or seconds, depending on the value
	 *         of {@link #usesTimeBasedWindow()}.
	 */
	public abstract int getWindowLength();

	/**
	 * Creates a new empty location with the specified timestamp.
	 * 
	 * @param timestamp
	 *            Timestamp, in milliseconds, associated with the new location.
	 */
	public abstract Location newLocation(double timestamp);

	/**
	 * Returns the percentage of the window that must be considered stationary
	 * for a new location to be created.
	 */
	public abstract float pctOfWindowRequiredToBeStationary();

	/**
	 * Returns true if the algorithm is using a fixed time length for the
	 * window, or false if the window contains a fixed number of samples.
	 */
	public abstract boolean usesTimeBasedWindow();
}
