/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import ca.mcgill.hs.util.LRUCache;
import ca.mcgill.hs.util.Log;

/**
 * Manages a set of Wifi-based Locations
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public final class WifiLocationSet extends LocationSet {

	/**
	 * Handles the creation and updating of the schema for the wifi location
	 * database.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 */
	private static class WifiDatabaseHelper extends SQLiteOpenHelper {

		public WifiDatabaseHelper(final Context context, final String name,
				final CursorFactory factory, final int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			/* Locations Table */
			db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE);
			db.execSQL("CREATE TABLE " + LOCATIONS_TABLE + " ( "
					+ "location_id	INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "timestamp 		TEXT, "
					+ "num_merged  INTEGER NOT NULL DEFAULT 1 " + ");");

			/* WAPs Table */
			db.execSQL("DROP TABLE IF EXISTS " + WAPS_TABLE);
			db.execSQL("CREATE TABLE " + WAPS_TABLE + " ( "
					+ "wap_id	INTEGER NOT NULL PRIMARY KEY, " + "BSSID	TEXT, "
					+ "SSID  	TEXT " + ");");

			/* Observations Table */
			db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			db.execSQL("CREATE TABLE " + OBSERVATIONS_TABLE + " ( "
					+ "location_id INTEGER NOT NULL REFERENCES "
					+ LOCATIONS_TABLE + ", "
					+ "wap_id INTEGER NOT NULL REFERENCES " + WAPS_TABLE + ", "
					+ "strength INTEGER NOT NULL DEFAULT 0, "
					+ "count INTEGER NOT NULL DEFAULT 0, "
					+ "average_strength FLOAT NOT NULL DEFAULT 0 " + ");");
			db.execSQL("CREATE UNIQUE INDEX idx_" + OBSERVATIONS_TABLE
					+ "_wap_id ON " + OBSERVATIONS_TABLE
					+ " (wap_id,location_id);");

			/* Neighbours Table */
			db.execSQL("DROP TABLE IF EXISTS " + NEIGHBOURS_TABLE);
			db.execSQL("CREATE TABLE " + NEIGHBOURS_TABLE + " ( "
					+ "location_id1 INTEGER NOT NULL REFERENCES "
					+ LOCATIONS_TABLE + ", "
					+ "location_id2 INTEGER NOT NULL REFERENCES "
					+ LOCATIONS_TABLE + " " + ");");
			db.execSQL("CREATE UNIQUE INDEX idx_" + NEIGHBOURS_TABLE
					+ "_loc1_loc2 ON " + NEIGHBOURS_TABLE
					+ " (location_id1,location_id2);");

			/* Labels Table */
			db.execSQL("DROP TABLE IF EXISTS " + LABELS_TABLE);
			db.execSQL("CREATE TABLE " + LABELS_TABLE + " ( "
					+ "label_id	INTEGER NOT NULL PRIMARY KEY, "
					+ "label		TEXT NOT NULL " + ");");

			/* Clusters Table */
			db.execSQL("DROP TABLE IF EXISTS " + CLUSTERS_TABLE);
			db.execSQL("CREATE TABLE " + CLUSTERS_TABLE + " ( "
					+ "location_id	INTEGER NOT NULL PRIMARY KEY REFERENCES "
					+ LOCATIONS_TABLE + ", " + "cluster_id		INTEGER NOT NULL, "
					+ "label_id		INTEGER REFERENCES " + LABELS_TABLE + " "
					+ ");");
			db.execSQL("CREATE UNIQUE INDEX idx_" + CLUSTERS_TABLE
					+ "_cluster_to_locations ON " + CLUSTERS_TABLE
					+ " (cluster_id,location_id);");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
		}

	}

	private static final String TAG = "WifiLocationSet";

	// Database table names.
	public static final String OBSERVATIONS_TABLE = "observations";
	public static final String WAPS_TABLE = "waps";

	/**
	 * The number of locations to cache.
	 */
	private static final int CACHE_SIZE = 100;
	/**
	 * Used to cache the last 100 most recently used locations, saves on
	 * database queries.
	 */
	private final LRUCache<Long, WifiLocation> locationCache = new LRUCache<Long, WifiLocation>(
			CACHE_SIZE);

	/**
	 * Do not use a time-based window, as samples can often come in at irregular
	 * intervals. This could use some tuning; it might work better if we use a
	 * time-based window but use the wifi-scanning interval value to pick a good
	 * time window.
	 */
	private static final boolean TIME_BASED_WINDOW = false;

	/**
	 * Use the last 6 samples for determining motion status.
	 */
	private static final int WINDOW_LENGTH = 6;

	/**
	 * Delta from the paper. This value represents the percentage of the points
	 * in the pool that must neighbours of a point for it to be considered to be
	 * part of a cluster.
	 */
	private static final float DELTA = 0.8f;

	private final WifiDatabaseHelper dbHelper;

	/**
	 * Controls whether we copy the database from the sdcard before opening it.
	 * This allows us, for debugging purposes, to modify the database offline
	 * and place it on the sdcard, to be used the next time the application is
	 * started.
	 */
	private final boolean copyFromSDCard = false;

	public WifiLocationSet(final Context context) {
		if (copyFromSDCard) {
			try {
				DBHelpers.copy(new File(
						"/sdcard/hsandroidapp/data/wificlusters.db"), new File(
						"/data/data/ca.mcgill.hs/databases/wificlusters.db"));
			} catch (final IOException e) {
				Log.e(TAG, e);
			}
			try {
				DBHelpers.copy(new File(
						"/sdcard/hsandroidapp/data/locations.db"), new File(
						"/data/data/ca.mcgill.hs/databases/locations.db"));
			} catch (final IOException e) {
				Log.e(TAG, e);
			}
		}
		dbHelper = new WifiDatabaseHelper(context, "wificlusters.db", null, 1);
		db = dbHelper.getWritableDatabase();
		Log.d(TAG, "Opened Wifi Database: " + db.getPath());

		// Uncomment for in-memory database
		// db = SQLiteDatabase.create(null);

		// uncomment to delete and recreate all of the tables.
		// dbHelper.onCreate(db);

		/*
		 * This should speed things up, as file i/o is slow. However, we should
		 * be aware that this can be dangerous, as crashes could result in a
		 * corrupted database.
		 */
		db.rawQuery("PRAGMA journal_mode=MEMORY", null).close();

		/*
		 * Does a little optimization when the service is started. Can't hurt.
		 */
		db.execSQL("ANALYZE");
	}

	@Override
	public long add(final Location loc) {
		final WifiLocation location = (WifiLocation) loc;
		final long location_id = location.getId();

		// Add the location to the cache.
		cacheLocation(location);

		/*
		 * Compute a threshold for minumum number of waps that must be common to
		 * be considered a neighbour. Note that if the neighbour has more waps,
		 * then it might have to be pruned out later. This is just used to
		 * retrieve from the database a set of potential neighbours, to quickly
		 * check if the location is a potential neighbour.
		 */
		final Long temp_threshold = (long) (location.getNumObservations() * WifiLocation.ETA);

		final Collection<Long> possibleNeighbours = new HashSet<Long>();

		/*
		 * Select all locations that have at least threshold neighbours in
		 * common with our current location.
		 */
		final Cursor cursor = db.rawQuery("SELECT o2.location_id FROM "
				+ OBSERVATIONS_TABLE + " AS o1 " + "JOIN " + OBSERVATIONS_TABLE
				+ " AS o2 USING (wap_id) JOIN " + LOCATIONS_TABLE
				+ " AS l USING (location_id) WHERE o1.location_id=? "
				+ "GROUP BY o2.location_id HAVING SUM(l.num_merged) > "
				+ temp_threshold,
				new String[] { Long.toString(location.getId()) });
		try {
			while (cursor.moveToNext()) {
				possibleNeighbours.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		// Make sure the current location isn't in the list of neighbours.
		possibleNeighbours.remove(location_id);

		Log.d(TAG, "Adding " + possibleNeighbours.size()
				+ " possible neighbours.");

		/*
		 * Now prune the list.
		 */
		final Collection<Long> neighboursToRemove = new HashSet<Long>();
		double min_dist = Double.MAX_VALUE;
		long nearest_neighbour_id = -1;
		for (final Long neighbour_id : possibleNeighbours) {
			final WifiLocation neighbour = (WifiLocation) getLocation(neighbour_id);
			final double dist = location.distanceFrom(neighbour);
			DebugHelper.out.println("\tDistance between " + location.getId()
					+ " and " + neighbour_id + " is " + dist);

			// Keep track of minimum distance to any neighbour
			if (dist < min_dist) {
				min_dist = dist;
				nearest_neighbour_id = neighbour_id;
			}
			if (dist > 0.0f && dist < WifiLocation.EPS) {
				// Add as neighbour's neighbour.
				// neighbour.addNeighbour(location_id);
			} else {
				neighboursToRemove.add(neighbour_id);
			}
		}
		if (min_dist < WifiLocation.MERGE_DIST) {
			mergeLocations(location,
					(WifiLocation) getLocation(nearest_neighbour_id));
			return nearest_neighbour_id;
		}
		possibleNeighbours.removeAll(neighboursToRemove);
		for (final Long neighbour_id : possibleNeighbours) {
			getLocation(neighbour_id).addNeighbour(location_id);
		}
		location.addNeighbours(possibleNeighbours);
		return location_id;
	}

	/**
	 * Adds a location to the cache.
	 * 
	 * @params location The location to be added to the cache.
	 */
	private void cacheLocation(final WifiLocation location) {
		locationCache.put(location.getId(), location);
	}

	public void close() {
		try {
			if (db != null) {
				final File dbFile = new File(db.getPath());
				db.close(); // Close the database handle
				dbHelper.close();

				/*
				 * We copy the database on the sdcard, since the phone
				 * permissions won't let us retrieve the database using adb. So
				 * this is mostly for debugging, if we want to look at the
				 * database offline.
				 */
				DBHelpers.copy(dbFile, new File(
						"/sdcard/hsandroidapp/data/wificlusters.db"));

				DBHelpers.copy(new File(
						"/data/data/ca.mcgill.hs/databases/locations.db"),
						new File("/sdcard/hsandroidapp/data/locations.db"));
			}
		} catch (final IOException e) {
			Log.e(TAG, e);
		} finally {
			db = null;
		}
	}

	/**
	 * 
	 * @param id
	 *            Location id to display
	 * @return Description of the location.
	 */
	public String displayLocation(final long id) {
		WifiLocation loc = null;
		loc = new WifiLocation(db, id);
		return loc.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public Location getLocation(final long id) {
		WifiLocation obs = locationCache.get(id);
		if (obs == null) {
			obs = new WifiLocation(db, id);
			cacheLocation(obs);
		}
		return obs;
	}

	@Override
	public int getWindowLength() {
		return WINDOW_LENGTH;
	}

	/**
	 * Merges the location src into dst, removing src and updating the
	 * num_merged for dst.
	 * 
	 * @param src
	 *            Location to be merged
	 * @param dst
	 *            Location that will hold the merged locations.
	 */
	private void mergeLocations(final WifiLocation src, final WifiLocation dst) {
		final long src_id = src.getId();
		final long dst_id = dst.getId();
		Log.d(TAG, "MERGING LOCATIONS " + src_id + " and " + dst_id);
		dst.addObservation(src.getObservations());
		dst.setNumMerged(dst.getNumMerged() + src.getNumMerged());
		db.execSQL("DELETE FROM " + WifiLocationSet.OBSERVATIONS_TABLE
				+ " WHERE location_id=?",
				new String[] { Long.toString(src_id) });
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? OR location_id2=?", new String[] {
				Long.toString(src_id), Long.toString(src_id) });
		db.execSQL("DELETE FROM " + LocationSet.LOCATIONS_TABLE
				+ " WHERE location_id=?",
				new String[] { Long.toString(src_id) });
		dst.removeNeighbour(src_id);
		// Remove from the cache.
		locationCache.put(src_id, null);
	}

	@Override
	public Location newLocation(final double timestamp) {
		return new WifiLocation(db, timestamp);
	}

	@Override
	public float pctOfWindowRequiredToBeStationary() {
		return DELTA;
	}

	/**
	 * Returns the number of locations that have been merged into this location.
	 * This should be used for computing the number of "neighbours" of a
	 * location, because if this is >1, then it means that this location
	 * actually represents more than one location, and should count as more than
	 * one neighbour.
	 * 
	 * @param location_id
	 *            The id of the location being queried.
	 * @return The number of locations that have been merged into the specified
	 *         location.
	 */
	public long retrieveNumMerged(final long location_id) {
		WifiLocation location = locationCache.get(location_id);
		if (location == null) {
			location = new WifiLocation(db, location_id);
			cacheLocation(location);
		}
		return location.getNumMerged();
	}

	@Override
	public boolean usesTimeBasedWindow() {
		return TIME_BASED_WINDOW;
	}
}
