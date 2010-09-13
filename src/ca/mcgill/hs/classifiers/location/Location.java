package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public abstract class Location {

	private static final String TAG = "Location";
	/**
	 * Locations can be merged, this keeps track of how many locations have been
	 * merged to create this current one
	 **/
	protected int num_merged = 1;
	private int location_id = -1;

	protected SQLiteDatabase db = null;
	protected double timestamp = -1;

	/**
	 * Creates a new location with the specified timestamp, and adds it to the
	 * database
	 * 
	 * @param db
	 * @param timestamp
	 * @throws SQLException
	 */
	public Location(final SQLiteDatabase db, final double timestamp) {
		this.db = db;
		this.location_id = generateNewLocationId();
		this.setTimestamp(timestamp);
	}

	/**
	 * Creates a location that is linked to a record in the database.
	 * 
	 * @param conn
	 * @param id
	 * @throws SQLException
	 */
	public Location(final SQLiteDatabase db, final int id) {
		location_id = id;

		this.db = db;
		this.setTimestamp(getTimestamp());
	}

	/**
	 * Adds a point as this point's neighbour.
	 * 
	 * @param point
	 *            A neighbouring point.
	 */
	public void addNeighbour(final int id) {
		if (id == getId()) {
			return;
		}
		final ContentValues values = new ContentValues();
		values.put("location_id1", getId());
		values.put("location_id2", id);
		values.put("distance", -1.0);
		db.insertWithOnConflict(LocationSet.NEIGHBOURS_TABLE, null, values,
				SQLiteDatabase.CONFLICT_IGNORE);
		// db.execSQL("INSERT OR IGNORE INTO " + LocationSet.NEIGHBOURS_TABLE
		// + " VALUES (" + getId() + "," + id + ")");
	}

	/**
	 * Adds a set of neighbours to the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void addNeighbours(final Collection<Integer> ids) {
		final SQLiteStatement stmt = db
				.compileStatement("INSERT OR IGNORE INTO "
						+ LocationSet.NEIGHBOURS_TABLE + " VALUES (?,?,-1.0)");
		try {
			stmt.bindLong(1, getId());
			for (final int id : ids) {
				if (id == getId()) {
					continue;
				}
				stmt.bindLong(2, id);
				stmt.execute();
				// db.execSQL("INSERT OR IGNORE INTO "
				// + LocationSet.NEIGHBOURS_TABLE + " VALUES (" + getId()
				// + "," + id + ")");
			}
		} finally {
			stmt.close();
		}
	}

	/**
	 * Adds an observation to this location. Typically these observations will
	 * be averaged out to form an average observation for the location.
	 * 
	 * @param observation
	 */
	public abstract void addObservation(Observation observation);

	public abstract double distanceFrom(Location other);

	/**
	 * Creates a new location record
	 * 
	 * @param conn
	 * @return
	 */
	protected int generateNewLocationId() {
		// Insert Default Values
		final int new_id = (int) db.insert(LocationSet.LOCATIONS_TABLE,
				"timestamp", null);
		return new_id;
	}

	public int getId() {
		return location_id;
	}

	/**
	 * Returns this point's neighbours.
	 * 
	 * @return This point's neighbours.
	 */
	public List<Integer> getNeighbours() {
		final List<Integer> neighbours = new LinkedList<Integer>();
		final String[] columns = { "location_id2" };
		final String[] params = { Integer.toString(getId()) };
		final Cursor cursor = db.query(LocationSet.NEIGHBOURS_TABLE, columns,
				"location_id1=?", params, null, null, null);

		// db.rawQuery("SELECT location_id2 FROM "
		// + LocationSet.NEIGHBOURS_TABLE + " WHERE location_id1=?",
		// params);
		try {
			while (cursor.moveToNext()) {
				neighbours.add(cursor.getInt(0));
			}
		} finally {
			cursor.close();
		}
		return neighbours;
	}

	public abstract Observation getObservations();

	/**
	 * Returns the timestamp.
	 * 
	 * @return The timestamp.
	 */
	public double getTimestamp() {
		if (timestamp < 0) {
			final String[] params = { Integer.toString(getId()) };
			final String[] columns = { "strftime('%s',timestamp)-strftime('%S',timestamp)+strftime('%f',timestamp)" };
			final Cursor cursor = db.query(LocationSet.LOCATIONS_TABLE,
					columns, "location_id=?", params, null, null, null);

			// final Cursor cursor = db
			// .rawQuery(
			// "SELECT strftime('%s',timestamp)-strftime('%S',timestamp)+strftime('%f',timestamp) FROM "
			// + LocationSet.LOCATIONS_TABLE
			// + " WHERE location_id=?", params);
			try {
				cursor.moveToNext();
				timestamp = cursor.getDouble(0);
			} finally {
				cursor.close();
			}
		}
		return timestamp;
	}

	/**
	 * Removes a point as this point's neighbour.
	 * 
	 * @param point
	 *            A neighbouring point.
	 */
	public void removeNeighbour(final int id) {
		final String[] params = { Integer.toString(getId()),
				Integer.toString(id) };
		// db.delete(LocationSet.NEIGHBOURS_TABLE,
		// "location_id1=? AND location_id2=?", params);
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?", params);

	}

	/**
	 * Removes a set of neighbours from the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void removeNeighbours(final Collection<Integer> neighboursToRemove) {
		final SQLiteStatement stmt = db.compileStatement("DELETE FROM "
				+ LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?");
		try {
			stmt.bindLong(1, getId());
			for (final int id : neighboursToRemove) {
				stmt.bindLong(2, id);
				stmt.execute();
				// db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				// + " WHERE location_id1=" + getId()
				// + " AND location_id2=" + id);
			}
		} finally {
			stmt.close();
		}

	}

	public void setTimestamp(final double timestamp) {
		this.timestamp = timestamp;
		// final ContentValues values = new ContentValues();
		// values.put("timestamp", "strftime(" + LocationSet.SQLITE_DATE_FORMAT
		// + "," + timestamp + ",'unixepoch')");
		// final String[] whereArgs = { Integer.toString(getId()) };
		// db.update(LocationSet.LOCATIONS_TABLE, values, "location_id=?",
		// whereArgs);
		final Object[] params = { timestamp, getId() };
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET timestamp=strftime(" + LocationSet.SQLITE_DATE_FORMAT
				+ ",?,'unixepoch') " + "WHERE location_id=?", params);
	}
}
