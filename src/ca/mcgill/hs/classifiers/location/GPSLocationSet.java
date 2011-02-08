/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ca.mcgill.hs.util.LRUCache;
import ca.mcgill.hs.util.Log;

/**
 * A set containing GPS locations.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class GPSLocationSet extends LocationSet {

	/**
	 * The number of locations to cache.
	 */
	private static final int CACHE_SIZE = 100;
	/**
	 * Used to cache the last 100 most recently used locations, saves on
	 * database queries.
	 */
	private final LRUCache<Long, GPSLocation> locationCache = new LRUCache<Long, GPSLocation>(
			CACHE_SIZE);

	private static final boolean TIME_BASED_WINDOW = false;

	/**
	 * Window length, in samples.
	 */
	private static final int WINDOW_LENGTH = 20;

	/**
	 * Delta from the paper. This value represents the percentage of the points
	 * in the pool that must neighbours of a point for it to be considered to be
	 * part of a cluster.
	 */
	private static final float DELTA = 0.8f;

	private static final String TAG = "GPSLocationSet";

	/**
	 * Constructor, connects to the database for GPS locations.
	 * 
	 * @param dbFile
	 *            The SQLite database file for the GPS locations.
	 */
	public GPSLocationSet(final File dbFile) {
		final boolean dbExists = dbFile.exists();

		db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

		Log.d(TAG, "Opening Database at " + dbFile.getPath());
		if (!dbExists) {
			final long startTime = System.currentTimeMillis();
			createDatabase(db);
			System.out.println("Took "
					+ (System.currentTimeMillis() - startTime)
					+ "ms to create database");
		}
	}

	@Override
	public long add(final Location loc) {
		final GPSLocation location = (GPSLocation) loc;

		cacheLocation(location);

		// Now get potential neighbours
		final Collection<Long> possibleNeighbours = new LinkedList<Long>();
		final double lat = location.getLatitude();
		final double lon = location.getLongitude();
		final Cursor cursor = db.rawQuery("SELECT location_id FROM locations "
				+ "WHERE latitude_average > " + (lat - GPSLocation.EPS)
				+ " AND latitude_average < " + (lat + GPSLocation.EPS)
				+ " AND longitude_average > " + (lon - GPSLocation.EPS)
				+ " AND longitude_average < " + (lon + GPSLocation.EPS) + ";",
				null);
		try {
			while (cursor.moveToNext()) {
				possibleNeighbours.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		// DebugHelper.out.println("Adding " + possibleNeighbours.size() +
		// " possible neighbours.");
		location.addNeighbours(possibleNeighbours);

		// Add 'o' as a neighbour to 'obs' if they are close, else remove 'obs'
		// as a neighbour of 'o'. Note that we assumed 'obs' was 'o''s neighbour
		// earlier; now we adjust that assumption.

		final Collection<Long> neighboursToRemove = new LinkedList<Long>();
		for (final Long neighbour_id : location.getNeighbours()) {
			final GPSLocation obs = (GPSLocation) getLocation(neighbour_id);
			final double dist = location.distanceFrom(obs);
			// DebugHelper.out.println("\tDistance between " + location.getId()
			// + " and " + neighbour_id + " is " + dist);
			if (dist > 0 && dist < GPSLocation.EPS) {
				obs.addNeighbour(neighbour_id);
			} else {
				neighboursToRemove.add(neighbour_id);
			}
		}
		location.removeNeighbours(neighboursToRemove);
		return location.getId();
	}

	/**
	 * Adds a location to the cache.
	 * 
	 * @params location The location to be added to the cache.
	 */
	private void cacheLocation(final GPSLocation location) {
		locationCache.put(location.getId(), location);
	}

	public void close() {
		try {
			if (db != null) {
				db.close(); // Close the database handle
			}
		} finally {
			db = null;
		}
	}

	/**
	 * Creates the initial GPS Clustering database. Note that if the database
	 * already contains information, it will be lost, as the tables are dropped
	 * before being created.
	 * 
	 * @param db
	 *            The database in which to create the tables.
	 */
	private void createDatabase(final SQLiteDatabase db) {
		/* Locations Table */
		db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE);
		db.execSQL("CREATE TABLE " + LOCATIONS_TABLE + " ( "
				+ "location_id	INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "timestamp 		TEXT DEFAULT (strftime(" + SQLITE_DATE_FORMAT
				+ ",'now')), " + "latitude_total		FLOAT NOT NULL DEFAULT 0, "
				+ "latitude_weights	FLOAT NOT NULL DEFAULT 0, "
				+ "latitude_average	FLOAT NOT NULL DEFAULT 0, "
				+ "longitude_total	FLOAT NOT NULL DEFAULT 0, "
				+ "longitude_weights	FLOAT NOT NULL DEFAULT 0, "
				+ "longitude_average	FLOAT NOT NULL DEFAULT 0, " +
				// "timestamp		DATETIME DEFAULT datetime('now')) , "
				// +
				"num_merged  INTEGER NOT NULL DEFAULT 1 " + ");");
		db
				.execSQL("CREATE TRIGGER update_average_positions AFTER UPDATE OF "
						+ "latitude_total,latitude_weights,longitude_total,longitude_weights "
						+ "ON "
						+ LOCATIONS_TABLE
						+ " FOR EACH ROW "
						+ "BEGIN "
						+ "UPDATE "
						+ LOCATIONS_TABLE
						+ " SET "
						+ "latitude_total = OLD.latitude_total + NEW.latitude_total*NEW.latitude_weights, "
						+ "latitude_weights = OLD.latitude_weights + NEW.latitude_weights, "
						+ "latitude_average = (OLD.latitude_total + NEW.latitude_total * NEW.latitude_weights) / (OLD.latitude_weights + NEW.latitude_weights), "
						+ "longitude_total = OLD.longitude_total + NEW.longitude_total * NEW.longitude_weights, "
						+ "longitude_weights = OLD.longitude_weights + NEW.longitude_weights, "
						+ "longitude_average = (OLD.longitude_total + NEW.longitude_total * NEW.longitude_weights) / (OLD.longitude_weights + NEW.longitude_weights) "
						+ "WHERE location_id = NEW.location_id; " + "END;");
		db.execSQL("CREATE INDEX idx_" + LOCATIONS_TABLE + "_latitude ON "
				+ LOCATIONS_TABLE + " (latitude_average);");
		db.execSQL("CREATE INDEX idx_" + LOCATIONS_TABLE + "_longitude ON "
				+ LOCATIONS_TABLE + " (longitude_average);");

		/* Neighbours Table */
		db.execSQL("DROP TABLE IF EXISTS " + NEIGHBOURS_TABLE);
		db.execSQL("CREATE TABLE " + NEIGHBOURS_TABLE + " ( "
				+ "location_id1 INTEGER NOT NULL REFERENCES " + LOCATIONS_TABLE
				+ ", " + "location_id2 INTEGER NOT NULL REFERENCES "
				+ LOCATIONS_TABLE + " " + ");");
		db.execSQL("CREATE UNIQUE INDEX idx_" + NEIGHBOURS_TABLE
				+ "_loc1_loc2 ON " + NEIGHBOURS_TABLE
				+ " (location_id1,location_id2);");
		db.execSQL("CREATE TRIGGER add_symmetric_neighbour AFTER INSERT ON "
				+ NEIGHBOURS_TABLE + " FOR EACH ROW " + "BEGIN "
				+ "INSERT INTO " + NEIGHBOURS_TABLE
				+ " VALUES (NEW.location_id2, NEW.location_id1); " + "END;");
		db
				.execSQL("CREATE TRIGGER remove_symmetric_neighbour AFTER DELETE ON "
						+ NEIGHBOURS_TABLE
						+ " FOR EACH ROW "
						+ "BEGIN "
						+ "DELETE FROM "
						+ NEIGHBOURS_TABLE
						+ " WHERE location_id1=OLD.location_id2 AND location_id2=OLD.location_id1; "
						+ "END;");

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
				+ "label_id		INTEGER REFERENCES " + LABELS_TABLE + " " + ");");
		db.execSQL("CREATE UNIQUE INDEX idx_" + CLUSTERS_TABLE
				+ "_cluster_to_locations ON " + CLUSTERS_TABLE
				+ " (cluster_id,location_id);");
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public Location getLocation(final long locationId) {
		GPSLocation obs = locationCache.get(locationId);
		if (obs == null) {
			obs = new GPSLocation(db, locationId);
			cacheLocation(obs);
		}
		return obs;
	}

	@Override
	public int getWindowLength() {
		return WINDOW_LENGTH;
	}

	@Override
	public Location newLocation(final double timestamp) {
		return new GPSLocation(db, timestamp);
	}

	@Override
	public float pctOfWindowRequiredToBeStationary() {
		return DELTA;
	}

	@Override
	public boolean usesTimeBasedWindow() {
		return TIME_BASED_WINDOW;
	}

}
