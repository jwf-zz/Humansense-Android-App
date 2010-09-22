package ca.mcgill.hs.classifiers.location;

import java.util.Collection;
import java.util.LinkedList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class LocationSet {

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

	public void addToCluster(final long locationId, final long clusterId) {
		db.execSQL("REPLACE INTO " + CLUSTERS_TABLE + " VALUES (" + locationId
				+ "," + clusterId + ", NULL);");
	}

	public abstract void cacheLocation(Location location);

	public void changeClusterId(final long oldId, final long newId) {
		if (oldId == newId) {
			return;
		}
		DebugHelper.out.println("\tChanging cluster " + oldId + " to cluster "
				+ newId);
		db.execSQL("UPDATE " + CLUSTERS_TABLE + " SET cluster_id=" + newId
				+ " WHERE cluster_id=" + oldId + ";");
	}

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
	 * Gets the cluster id for a particular location_id.
	 * 
	 * @param location_id
	 * @return Cluster id, or -1 if point is not in a cluster.
	 */
	public long getClusterId(final long location_id) {
		long cluster_id = -1;
		final Cursor cursor = db.rawQuery("SELECT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id=?;", new String[] { Long
				.toString(location_id) });
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
	 *            the set of locations to check
	 * @return the cluster ids that occur in the collection of locations.
	 */
	public Collection<Long> getClusterIds(final Collection<Long> locations) {
		final StringBuilder location_ids = new StringBuilder();
		boolean first = true;
		for (final long location_id : locations) {
			if (first) {
				location_ids.append(location_id);
				first = false;
			} else {
				location_ids.append("," + location_id);
			}
		}

		final Collection<Long> clusters = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT DISTINCT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id IN ("
				+ location_ids.toString() + ")", null);
		try {
			while (cursor.moveToNext()) {
				clusters.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return clusters;
	}

	public abstract Location getLocation(long location_id);

	public Collection<Long> getLocationsForCluster(final long cluster_id) {
		final Collection<Long> ids = new LinkedList<Long>();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM "
				+ CLUSTERS_TABLE + " WHERE cluster_id=?", new String[] { Long
				.toString(cluster_id) });
		try {
			while (cursor.moveToNext()) {
				ids.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return ids;
	}

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

	public abstract int getWindowLength();

	public abstract Location newLocation(double timestamp);

	public abstract float pctOfWindowRequiredToBeStationary();

	public abstract boolean usesTimeBasedWindow();
}
