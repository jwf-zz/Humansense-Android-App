package ca.mcgill.hs.classifiers.location;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import ca.mcgill.hs.util.LRUCache;

/**
 * Manages a set of Wifi-based Location fingerprints
 */
public final class WifiLocationSet extends LocationSet {

	/**
	 * Handles the creation and updating of the schema for the wifi location
	 * database.
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

	/**
	 * Database table names.
	 */
	public static final String OBSERVATIONS_TABLE = "observations";
	public static final String WAPS_TABLE = "waps";

	/**
	 * Used to cache the most recent locations, avoids some database calls. We
	 * currently cache the last 100 most recently used locations.
	 */
	private final LRUCache<Integer, WifiLocation> locationCache = new LRUCache<Integer, WifiLocation>(
			100);

	/**
	 * Do not use a time-based window, as samples can often come in at irregular
	 * intervals.
	 */
	private static final boolean TIME_BASED_WINDOW = false;

	/**
	 * Use the last 6 samples for determining motion status.
	 */
	private static final int WINDOW_LENGTH = 6;

	/**
	 * Delta from the paper. This value represents the percentage of the points
	 * in the pool that must neighbours of a point for it to be considered to be
	 * part of a cluster
	 */
	private static final float DELTA = 0.8f;

	private final WifiDatabaseHelper dbHelper;

	/**
	 * Prepared statement for retrieving the number of merged locations. This is
	 * used quite frequently but we have not actually benchmarked prepared
	 * statements, and it is possible that this doesn't speed anything up.
	 */
	SQLiteStatement retrieveNumMergedStmt = null;

	public WifiLocationSet(final Context context) {
		try {
			/*
			 * We currently copy the database from the sdcard. This is mainly
			 * used for debugging so that we can make changes to the database
			 * offline and then use those changes next time we start the
			 * service. This should eventually be removed.
			 */
			DBHelpers
					.copy(
							new File(
									"/sdcard/hsandroidapp/data/wificlusters.db"),
							new File(
									"/data/data/ca.mcgill.hs/databases/wificlusters.db"));
		} catch (final IOException e) {
			e.printStackTrace();
		}

		dbHelper = new WifiDatabaseHelper(context, "wificlusters.db", null, 1);
		db = dbHelper.getWritableDatabase();

		// Uncomment for in-memory database
		// db = SQLiteDatabase.create(null);

		// uncomment to recreate databases
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
		retrieveNumMergedStmt = db.compileStatement("SELECT num_merged FROM "
				+ LocationSet.LOCATIONS_TABLE + " WHERE location_id=?");
	}

	@Override
	public int add(final Location loc) {
		final WifiLocation location = (WifiLocation) loc;
		final int location_id = location.getId();

		/*
		 * Compute a threshold for minumum number of waps that must be common to
		 * be considered a neighbour. Note that if the neighbour has more waps,
		 * then it might have to be pruned out later. This is just used to
		 * retrieve from the database a set of potential neighbours.
		 */
		final Integer temp_threshold = (int) (location.numObservations() * WifiLocation.ETA);

		final Collection<Integer> possibleNeighbours = new HashSet<Integer>();

		final String[] params = { Integer.toString(location.getId()) };

		/*
		 * Select all locations that have at least threshold neighbours in
		 * common with our current location.
		 */
		final Cursor cursor = db.rawQuery("SELECT o2.location_id FROM "
				+ OBSERVATIONS_TABLE + " AS o1 " + "JOIN " + OBSERVATIONS_TABLE
				+ " AS o2 USING (wap_id) JOIN " + LOCATIONS_TABLE
				+ " AS l USING (location_id) WHERE o1.location_id=? "
				+ "GROUP BY o2.location_id HAVING SUM(l.num_merged) > "
				+ temp_threshold, params);
		try {
			while (cursor.moveToNext()) {
				possibleNeighbours.add(cursor.getInt(0));
			}
		} finally {
			cursor.close();
		}
		// Remove current location from the neighbours list.
		try {
			possibleNeighbours.remove(location_id);
		} catch (final IndexOutOfBoundsException e) {
			// Ignore.
		}
		Log.d(TAG, "Adding " + possibleNeighbours.size()
				+ " possible neighbours.");

		/*
		 * Now prune the list.
		 */
		final Collection<Integer> neighboursToRemove = new HashSet<Integer>();
		for (final Integer neighbour_id : possibleNeighbours) {

			final WifiLocation neighbour = (WifiLocation) getLocation(neighbour_id);
			final double dist = location.distanceFrom(neighbour);
			DebugHelper.out.println("\tDistance between " + location.getId()
					+ " and " + neighbour_id + " is " + dist);

			// Merge location if it is very close to a possible neighbour.
			if (dist < WifiLocation.MERGE_DIST) {
				mergeLocations(location, neighbour);
				return neighbour_id;
			}

			if (dist > 0 && dist < WifiLocation.EPS) {
				// Add as neighbour's neighbour.
				neighbour.addNeighbour(location_id);
			} else {
				neighboursToRemove.add(neighbour_id);
			}
		}
		possibleNeighbours.removeAll(neighboursToRemove);
		location.addNeighbours(possibleNeighbours);
		return location_id;
	}

	@Override
	public void cacheLocation(final Location location) {
		locationCache.put(location.getId(), (WifiLocation) location);
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

			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			db = null;
		}
	}

	public String displayLocation(final int id) {
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
	public Location getLocation(final int id) {
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
		final int src_id = src.getId();
		final int dst_id = dst.getId();
		Log.d(TAG, "MERGING LOCATIONS " + src_id + " and " + dst_id);
		dst.addObservation(src.getObservations());
		dst.setNumMerged(dst.getNumMerged() + src.getNumMerged());
		db.execSQL("DELETE FROM " + WifiLocationSet.OBSERVATIONS_TABLE
				+ " WHERE location_id=?", new String[] { Integer
				.toString(src_id) });
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? OR location_id2=?", new String[] {
				Integer.toString(src_id), Integer.toString(src_id) });
		db.execSQL("DELETE FROM " + LocationSet.LOCATIONS_TABLE
				+ " WHERE location_id=?", new String[] { Integer
				.toString(src_id) });
		// Remove from the cache.
		locationCache.put(src_id, null);
	}

	@Override
	public Location newLocation(final double timestamp) {
		return new WifiLocation(db, timestamp);
	}

	/**
	 * Returns the DELTA parameter, or the fraction of the buffer that must be
	 * part of a cluster in order for the user to be considered stationary.
	 */
	@Override
	public float pctOfWindowRequiredToBeStationary() {
		return DELTA;
	}

	/**
	 * Returns the number of locations that have been merged into this location.
	 * This should be used for computing the number of "neighborus" of a
	 * location, because if this is >1, then it means that this location
	 * actually represents more than one location, and should count as more than
	 * one neighbour.
	 * 
	 * @param location_id
	 *            The id of the location being queried.
	 * @return The number of locations that have been merged into the specified
	 *         location.
	 */
	public int retrieveNumMerged(final int location_id) {
		retrieveNumMergedStmt.bindLong(0, location_id);
		return (int) retrieveNumMergedStmt.simpleQueryForLong();
	}

	/**
	 * Returns a value specifying whether the MotionStateClassifier should use a
	 * time-based window.
	 */
	@Override
	public boolean usesTimeBasedWindow() {
		return TIME_BASED_WINDOW;
	}
}
