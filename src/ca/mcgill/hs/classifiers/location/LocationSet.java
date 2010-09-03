package ca.mcgill.hs.classifiers.location;

import java.util.Collection;
import java.util.LinkedList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class LocationSet {

	public static final String LOCATIONS_TABLE = "locations";
	public static final String OBSERVATIONS_TABLE = "observations";
	public static final String WAPS_TABLE = "waps";
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
	 */
	public abstract void add(Location point);

	public void addToCluster(final int locationId, final int clusterId) {
		DebugHelper.out.println("\tAdding " + locationId + " to cluster "
				+ clusterId);
		db.execSQL("REPLACE INTO " + CLUSTERS_TABLE + " VALUES (" + locationId
				+ "," + clusterId + ", NULL);");
	}

	public abstract void cacheLocation(Location location);

	public void changeClusterId(final int oldId, final int newId) {
		if (oldId == newId) {
			return;
		}
		DebugHelper.out.println("\tChanging cluster " + oldId + " to cluster "
				+ newId);
		db.execSQL("UPDATE " + CLUSTERS_TABLE + " SET cluster_id=" + newId
				+ " WHERE cluster_id=" + oldId + ";");
	}

	public Collection<Integer> getAllClusters() {
		final Collection<Integer> clusters = new LinkedList<Integer>();
		final Cursor cursor = db.rawQuery("SELECT DISTINCT cluster_id FROM "
				+ CLUSTERS_TABLE + ";", null);
		while (cursor.moveToNext()) {
			clusters.add(cursor.getInt(0));
		}
		return clusters;
	}

	public Collection<Integer> getAllLocations() {
		final Collection<Integer> locations = new LinkedList<Integer>();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM "
				+ LOCATIONS_TABLE + ";", null);
		while (cursor.moveToNext()) {
			locations.add(cursor.getInt(0));
		}
		return locations;
	}

	/**
	 * Gets the cluster id for a particular location_id.
	 * 
	 * @param location_id
	 * @return Cluster id, or -1 if point is not in a cluster.
	 */
	public int getClusterId(final int location_id) {
		int cluster_id = -1;
		final Cursor cursor = db.rawQuery("SELECT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id=" + location_id + ";",
				null);
		if (cursor.moveToNext()) {
			cluster_id = cursor.getInt(0);
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
	public Collection<Integer> getClusterIds(final Collection<Integer> locations) {
		final StringBuilder location_ids = new StringBuilder();
		boolean first = true;
		for (final int location_id : locations) {
			if (first) {
				location_ids.append(location_id);
				first = false;
			} else {
				location_ids.append("," + location_id);
			}
		}

		final Collection<Integer> clusters = new LinkedList<Integer>();
		final Cursor cursor = db.rawQuery("SELECT DISTINCT cluster_id FROM "
				+ CLUSTERS_TABLE + " WHERE location_id IN ("
				+ location_ids.toString() + ");", null);
		while (cursor.moveToNext()) {
			clusters.add(cursor.getInt(0));
		}
		return clusters;
	}

	public abstract Location getLocation(int location_id);

	public Collection<Integer> getLocationsForCluster(final int cluster_id) {
		final Collection<Integer> ids = new LinkedList<Integer>();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM "
				+ CLUSTERS_TABLE + " WHERE cluster_id=" + cluster_id + ";",
				null);
		while (cursor.moveToNext()) {
			ids.add(cursor.getInt(0));
		}
		return ids;
	}

	public int getNewClusterId() {
		int cluster_id = -1;
		final Cursor cursor = db.rawQuery("SELECT MAX(cluster_id) FROM "
				+ CLUSTERS_TABLE + ";", null);
		if (cursor.moveToNext()) {
			cluster_id = cursor.getInt(0) + 1;
		}
		return cluster_id;
	}

	public abstract int getWindowLength();

	public abstract Location newLocation(double timestamp);

	public abstract float pctOfWindowRequiredToBeStationary();

	public abstract boolean usesTimeBasedWindow();
}
