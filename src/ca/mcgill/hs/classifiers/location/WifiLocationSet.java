package ca.mcgill.hs.classifiers.location;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import ca.mcgill.hs.util.LRUCache;

public final class WifiLocationSet extends LocationSet {

	private static class WifiDatabaseHelper extends SQLiteOpenHelper {

		public WifiDatabaseHelper(final Context context, final String name,
				final CursorFactory factory, final int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			// /* Locations Table */
			// db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE);
			// db.execSQL("CREATE TABLE " + LOCATIONS_TABLE + " ( "
			// + "location_id	INTEGER PRIMARY KEY AUTOINCREMENT, "
			// + "timestamp 		TEXT, "
			// + "num_merged  INTEGER NOT NULL DEFAULT 1 " + ");");
			//
			// /* WAPs Table */
			// db.execSQL("DROP TABLE IF EXISTS " + WAPS_TABLE);
			// db.execSQL("CREATE TABLE " + WAPS_TABLE + " ( "
			// + "wap_id	INTEGER NOT NULL PRIMARY KEY, " + "BSSID	TEXT, "
			// + "SSID  	TEXT " + ");");
			//
			// /* Observations Table */
			// db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			// db.execSQL("CREATE TABLE " + OBSERVATIONS_TABLE + " ( "
			// + "location_id INTEGER NOT NULL REFERENCES "
			// + LOCATIONS_TABLE + ", "
			// + "wap_id INTEGER NOT NULL REFERENCES " + WAPS_TABLE + ", "
			// + "strength INTEGER NOT NULL DEFAULT 0, "
			// + "count INTEGER NOT NULL DEFAULT 0, "
			// + "average_strength FLOAT NOT NULL DEFAULT 0 " + ");");
			// db.execSQL("CREATE UNIQUE INDEX idx_" + OBSERVATIONS_TABLE
			// + "_wap_id ON " + OBSERVATIONS_TABLE
			// + " (wap_id,location_id);");
			// db
			// .execSQL("CREATE TRIGGER update_signal_strengths AFTER UPDATE ON "
			// + OBSERVATIONS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "UPDATE "
			// + OBSERVATIONS_TABLE
			// +
			// " SET strength=OLD.strength+NEW.strength, count=OLD.count+1, average_strength=(OLD.strength+NEW.strength)/(OLD.count+1) WHERE location_id=NEW.location_id AND wap_id=NEW.wap_id; "
			// + "END;");
			//
			// /* Neighbours Table */
			// db.execSQL("DROP TABLE IF EXISTS " + NEIGHBOURS_TABLE);
			// db.execSQL("CREATE TABLE " + NEIGHBOURS_TABLE + " ( "
			// + "location_id1 INTEGER NOT NULL REFERENCES "
			// + LOCATIONS_TABLE + ", "
			// + "location_id2 INTEGER NOT NULL REFERENCES "
			// + LOCATIONS_TABLE + ", "
			// + "distance FLOAT NOT NULL DEFAULT -1.0 " + ");");
			// db.execSQL("CREATE UNIQUE INDEX idx_" + NEIGHBOURS_TABLE
			// + "_loc1_loc2 ON " + NEIGHBOURS_TABLE
			// + " (location_id1,location_id2);");
			// db
			// .execSQL("CREATE TRIGGER add_symmetric_neighbour AFTER INSERT ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "INSERT INTO "
			// + NEIGHBOURS_TABLE
			// + " VALUES (NEW.location_id2, NEW.location_id1, NEW.distance); "
			// + "END;");
			// db
			// .execSQL("CREATE TRIGGER remove_symmetric_neighbour AFTER DELETE ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "DELETE FROM "
			// + NEIGHBOURS_TABLE
			// +
			// " WHERE location_id1=OLD.location_id2 AND location_id2=OLD.location_id1; "
			// + "END;");
			// db
			// .execSQL("CREATE TRIGGER symmetrize_distance AFTER UPDATE ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "UPDATE "
			// + NEIGHBOURS_TABLE
			// + " SET distance=NEW.distance "
			// +
			// "WHERE location_id1=OLD.location_id2 and location_id2=OLD.location_id1; "
			// + "END;");
			//
			// /* Labels Table */
			// db.execSQL("DROP TABLE IF EXISTS " + LABELS_TABLE);
			// db.execSQL("CREATE TABLE " + LABELS_TABLE + " ( "
			// + "label_id	INTEGER NOT NULL PRIMARY KEY, "
			// + "label		TEXT NOT NULL " + ");");
			//
			// /* Clusters Table */
			// db.execSQL("DROP TABLE IF EXISTS " + CLUSTERS_TABLE);
			// db.execSQL("CREATE TABLE " + CLUSTERS_TABLE + " ( "
			// + "location_id	INTEGER NOT NULL PRIMARY KEY REFERENCES "
			// + LOCATIONS_TABLE + ", " + "cluster_id		INTEGER NOT NULL, "
			// + "label_id		INTEGER REFERENCES " + LABELS_TABLE + " "
			// + ");");
			// db.execSQL("CREATE UNIQUE INDEX idx_" + CLUSTERS_TABLE
			// + "_cluster_to_locations ON " + CLUSTERS_TABLE
			// + " (cluster_id,location_id);");

			/*** NOW WITHOUT FOREIGN KEY CONSTRAINTS ***/
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
					+ "location_id INTEGER NOT NULL, "
					+ "wap_id INTEGER NOT NULL, "
					+ "strength INTEGER NOT NULL DEFAULT 0, "
					+ "count INTEGER NOT NULL DEFAULT 0, "
					+ "average_strength FLOAT NOT NULL DEFAULT 0 " + ");");
			db.execSQL("CREATE UNIQUE INDEX idx_" + OBSERVATIONS_TABLE
					+ "_wap_id ON " + OBSERVATIONS_TABLE
					+ " (wap_id,location_id);");
			// db
			// .execSQL("CREATE TRIGGER update_signal_strengths AFTER UPDATE OF strength ON "
			// + OBSERVATIONS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "UPDATE "
			// + OBSERVATIONS_TABLE
			// +
			// " SET strength=OLD.strength+NEW.strength, count=OLD.count+1, average_strength=(OLD.strength+NEW.strength)/(OLD.count+1) WHERE location_id=NEW.location_id AND wap_id=NEW.wap_id; "
			// + "END;");

			/* Neighbours Table */
			db.execSQL("DROP TABLE IF EXISTS " + NEIGHBOURS_TABLE);
			db.execSQL("CREATE TABLE " + NEIGHBOURS_TABLE + " ( "
					+ "location_id1 INTEGER NOT NULL, "
					+ "location_id2 INTEGER NOT NULL "
					// + "distance FLOAT NOT NULL DEFAULT -1.0 "
					+ ");");
			db.execSQL("CREATE UNIQUE INDEX idx_" + NEIGHBOURS_TABLE
					+ "_loc1_loc2 ON " + NEIGHBOURS_TABLE
					+ " (location_id1,location_id2);");
			// db
			// .execSQL("CREATE TRIGGER add_symmetric_neighbour AFTER INSERT ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "INSERT INTO "
			// + NEIGHBOURS_TABLE
			// + " VALUES (NEW.location_id2, NEW.location_id1, NEW.distance); "
			// + "END;");
			// db
			// .execSQL("CREATE TRIGGER remove_symmetric_neighbour AFTER DELETE ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "DELETE FROM "
			// + NEIGHBOURS_TABLE
			// +
			// " WHERE location_id1=OLD.location_id2 AND location_id2=OLD.location_id1; "
			// + "END;");
			// db
			// .execSQL("CREATE TRIGGER symmetrize_distance AFTER UPDATE OF distance ON "
			// + NEIGHBOURS_TABLE
			// + " FOR EACH ROW "
			// + "BEGIN "
			// + "UPDATE "
			// + NEIGHBOURS_TABLE
			// + " SET distance=NEW.distance "
			// +
			// "WHERE location_id1=OLD.location_id2 and location_id2=OLD.location_id1; "
			// + "END;");

			/* Labels Table */
			db.execSQL("DROP TABLE IF EXISTS " + LABELS_TABLE);
			db.execSQL("CREATE TABLE " + LABELS_TABLE + " ( "
					+ "label_id	INTEGER NOT NULL PRIMARY KEY, "
					+ "label		TEXT NOT NULL " + ");");

			/* Clusters Table */
			db.execSQL("DROP TABLE IF EXISTS " + CLUSTERS_TABLE);
			db.execSQL("CREATE TABLE " + CLUSTERS_TABLE + " ( "
					+ "location_id	INTEGER NOT NULL PRIMARY KEY, "
					+ "cluster_id	INTEGER NOT NULL, " + "label_id		INTEGER "
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

	public static final String OBSERVATIONS_TABLE = "observations";

	public static final String WAPS_TABLE = "waps";

	/** Fast & simple file copy. */
	public static void copy(final File source, final File dest)
			throws IOException {
		FileChannel in = null, out = null;
		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(dest).getChannel();

			final long size = in.size();
			final MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY,
					0, size);

			out.write(buf);

		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}

	// private Map<Integer,WifiLocation> locationCache = new
	// HashMap<Integer,WifiLocation>();
	private final LRUCache<Integer, WifiLocation> locationCache = new LRUCache<Integer, WifiLocation>(
			100);

	private static final boolean TIME_BASED_WINDOW = false;

	// Window length, in samples.
	private static final int WINDOW_LENGTH = 6;

	// Delta from the paper. This value represents the percentage of the points
	// in the pool that must neighbours of a point for it to be considered to be
	// part of a cluster
	private static final float DELTA = 0.8f;

	private static final String TAG = "WifiLocationSet";

	private final WifiDatabaseHelper dbHelper;

	SQLiteStatement retrieveNumMergedStmt = null;

	public WifiLocationSet(final Context context) {
		// try {
		// copy(
		// new File("/sdcard/hsandroidapp/data/wificlusters.db"),
		// new File(
		// "/data/data/ca.mcgill.hs/databases/wificlusters.db"));
		// } catch (final IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		dbHelper = new WifiDatabaseHelper(context, "wificlusters.db", null, 1);
		db = dbHelper.getWritableDatabase();

		// Uncomment for in-memory database
		// db = SQLiteDatabase.create(null);

		// uncomment to recreate databases
		dbHelper.onCreate(db);

		retrieveNumMergedStmt = db.compileStatement("SELECT num_merged FROM "
				+ LocationSet.LOCATIONS_TABLE + " WHERE location_id=?");
	}

	@Override
	public int add(final Location loc) {
		final WifiLocation location = (WifiLocation) loc;
		final int location_id = location.getId();
		// if (location.getId() == 50) {
		// Debug.stopMethodTracing();
		// }
		// Now get potential neighbours

		// Compute a threshold for minumum number of waps that must be common to
		// be considered
		// a neighbour. Note that if the neighbour has more waps, then it might
		// have to be pruned
		// out later.
		final Integer temp_threshold = (int) (location.numObservations() * WifiLocation.ETA);
		// DebugHelper.out.println("Id is: " + location.location_id +
		// ", Threshold is: " + temp_threshold);
		final Collection<Integer> possibleNeighbours = new HashSet<Integer>(
				(int) (temp_threshold / 0.75f) + 1);

		// final String table = OBSERVATIONS_TABLE + " as o1 " + "JOIN "
		// + OBSERVATIONS_TABLE + " as o2 USING (wap_id)";
		// final String[] columns = { "o2.location_id", "count(o2.location_id)"
		// };
		// final String selection = "o1.location_id=?";
		// final String[] selectionArgs = { Integer
		// .toString(location.getId()) };
		// final String groupBy = "o2.location_id";
		// final String having = "COUNT(o2.location_id) > " + temp_threshold;
		// final Cursor cursor = db.query(table, columns, selection,
		// selectionArgs, groupBy, having, null);

		final String[] params = { Integer.toString(location.getId()) };
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
		// Remove myself from myself's neighbours.
		try {
			possibleNeighbours.remove(location_id);
		} catch (final IndexOutOfBoundsException e) {
			// Ignore.
		}
		Log.d(TAG, "Adding " + possibleNeighbours.size()
				+ " possible neighbours.");
		// location.addNeighbours(possibleNeighbours);

		// Add 'o' as a neighbour to 'obs' if they are close, else remove 'obs'
		// as a neighbour of 'o'. Note that we assumed 'obs' was 'o''s neighbour
		// earlier; now we adjust that assumption.

		final Collection<Integer> neighboursToRemove = new HashSet<Integer>();
		for (final Integer neighbour_id : possibleNeighbours) {

			final WifiLocation neighbour = (WifiLocation) getLocation(neighbour_id);
			final double dist = location.distanceFrom(neighbour);
			DebugHelper.out.println("\tDistance between " + location.getId()
					+ " and " + neighbour_id + " is " + dist);

			// TODO: Get merging right.
			// if (dist < WifiLocation.MERGE_DIST) {
			// mergeLocations(location, neighbour);
			// return neighbour_id;
			// }
			if (dist > 0 && dist < WifiLocation.EPS) {
				// Add myself as a neighbour of my neighbour.
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
				dumpDBToFile();
				final File dbFile = new File(db.getPath());
				db.close(); // Close the database handle
				dbHelper.close();
				copy(dbFile, new File("/sdcard/hsandroidapp/data/"
						+ dbFile.getName()));

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

	private void dumpDBToFile() {
		final Date d = new Date(System.currentTimeMillis());
		final SimpleDateFormat dfm = new SimpleDateFormat("yy-MM-dd-HHmmss");
		final File f = new File("/sdcard/hsandroidapp/data/recent/"
				+ dfm.format(d) + "-clusters.log");
		try {
			final BufferedWriter br = new BufferedWriter(new FileWriter(f));
			final Cursor cursor = db.rawQuery(
					"SELECT l.timestamp,l.location_id,c.cluster_id FROM "
							+ LocationSet.LOCATIONS_TABLE + " AS l JOIN "
							+ LocationSet.CLUSTERS_TABLE
							+ " AS c USING (location_id) ORDER BY timestamp",
					null);
			try {
				while (cursor.moveToNext()) {
					br.write(cursor.getString(0) + "," + cursor.getInt(1) + ","
							+ cursor.getInt(2) + "\n");
				}
			} finally {
				cursor.close();
				br.flush();
				br.close();
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		final String[] params = { Integer.toString(src_id) };
		db.execSQL("DELETE FROM " + WifiLocationSet.OBSERVATIONS_TABLE
				+ " WHERE location_id=?", params);
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=?", params);
		db.execSQL("DELETE FROM " + LocationSet.LOCATIONS_TABLE
				+ " WHERE location_id=?", params);
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

	public int retrieveNumMerged(final int location_id) {
		retrieveNumMergedStmt.bindLong(0, location_id);
		return (int) retrieveNumMergedStmt.simpleQueryForLong();
	}

	@Override
	public boolean usesTimeBasedWindow() {
		return TIME_BASED_WINDOW;
	}
}
