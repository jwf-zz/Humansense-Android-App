package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public abstract class Location {

	private static final String TAG = "Location";
	/**
	 * Locations can be merged, this keeps track of how many locations have been
	 * merged to create this current one
	 **/
	private int num_merged = -1;
	private int location_id = -1;

	protected SQLiteDatabase db = null;
	protected double timestamp = -1;
	private int num_neighbours = -1;
	private List<Integer> neighbours = null;

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
		this.num_merged = 1;
		this.neighbours = new LinkedList<Integer>();
	}

	/**
	 * Creates a location that is linked to a record in the database.
	 * 
	 * @param conn
	 * @param id
	 * @throws SQLException
	 */
	public Location(final SQLiteDatabase db, final int id) {
		this.location_id = id;
		this.db = db;
		this.setTimestamp(getTimestamp());
		this.num_merged = -1;
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
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		neighbours.add(id);
		db.execSQL("INSERT OR IGNORE INTO " + LocationSet.NEIGHBOURS_TABLE
				+ " VALUES (?,?)", new String[] { Integer.toString(getId()),
				Integer.toString(id) });
		num_neighbours = -1;
	}

	/**
	 * Adds a set of neighbours to the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void addNeighbours(final Collection<Integer> ids) {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		neighbours.addAll(ids);
		final SQLiteStatement stmt = db
				.compileStatement("INSERT OR IGNORE INTO "
						+ LocationSet.NEIGHBOURS_TABLE + " VALUES (?,?)");
		try {
			db.beginTransaction();
			stmt.bindLong(1, getId());
			for (final int id : ids) {
				if (id == getId()) {
					try {
						neighbours.remove(id);
					} catch (final IndexOutOfBoundsException e) {
						// Ignore
					}

					continue;
				}
				stmt.bindLong(2, id);
				stmt.execute();
				// db.execSQL("INSERT OR IGNORE INTO "
				// + LocationSet.NEIGHBOURS_TABLE + " VALUES (" + getId()
				// + "," + id + ")");
			}
			db.setTransactionSuccessful();
		} finally {
			stmt.close();
			db.endTransaction();
			num_neighbours = -1;
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
		if (neighbours == null) {
			final List<Integer> neighbours = new LinkedList<Integer>();
			num_neighbours = 0;
			final Cursor cursor = db.rawQuery(
					"SELECT location_id2,num_merged FROM "
							+ LocationSet.NEIGHBOURS_TABLE + " JOIN "
							+ LocationSet.LOCATIONS_TABLE
							+ " ON location_id=location_id2 "
							+ "WHERE location_id1=?", new String[] { Integer
							.toString(getId()) });
			try {
				while (cursor.moveToNext()) {
					neighbours.add(cursor.getInt(0));
					num_neighbours += cursor.getInt(1);
				}
			} finally {
				cursor.close();
			}
			// Cache the neighbours
			this.neighbours = neighbours;
		}
		return neighbours;
	}

	public int getNumMerged() {
		if (num_merged > 0) {
			return num_merged;
		}
		final Cursor cursor = db.rawQuery("SELECT num_merged FROM "
				+ LocationSet.LOCATIONS_TABLE + " WHERE location_id=?",
				new String[] { Integer.toString(getId()) });
		try {
			cursor.moveToNext();
			num_merged = cursor.getInt(0);
		} finally {
			cursor.close();
		}
		return num_merged;
	}

	public int getNumNeighbours() {
		if (num_neighbours >= 0) {
			return num_neighbours;
		}
		final SQLiteStatement stmt = db
				.compileStatement("SELECT SUM(num_merged) FROM "
						+ LocationSet.NEIGHBOURS_TABLE + " JOIN "
						+ LocationSet.LOCATIONS_TABLE
						+ " ON location_id=location_id2 WHERE location_id1=?");
		try {
			stmt.bindLong(1, getId());
			num_neighbours = (int) stmt.simpleQueryForLong();
		} finally {
			stmt.close();
		}
		return num_neighbours;
	}

	public abstract Observation getObservations();

	/**
	 * Returns the timestamp.
	 * 
	 * @return The timestamp.
	 */
	public double getTimestamp() {
		if (timestamp < 0) {
			final Cursor cursor = db
					.rawQuery(
							"SELECT strftime('%s',timestamp)-strftime('%S',timestamp)+strftime('%f',timestamp) FROM "
									+ LocationSet.LOCATIONS_TABLE
									+ " WHERE location_id=?",
							new String[] { Integer.toString(getId()) });
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
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		try {
			neighbours.remove(id);
		} catch (final IndexOutOfBoundsException e) {
			// Ignore
		}
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?", new String[] {
				Integer.toString(getId()), Integer.toString(id) });
		num_neighbours = -1;
	}

	/**
	 * Removes a set of neighbours from the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void removeNeighbours(final Collection<Integer> neighboursToRemove) {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		final SQLiteStatement stmt = db.compileStatement("DELETE FROM "
				+ LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?");
		try {
			db.beginTransaction();
			stmt.bindLong(1, getId());
			for (final int id : neighboursToRemove) {
				try {
					neighbours.remove(id);
				} catch (final IndexOutOfBoundsException e) {
					// Ignore
				}
				stmt.bindLong(2, id);
				stmt.execute();
				// db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				// + " WHERE location_id1=" + getId()
				// + " AND location_id2=" + id);
			}
			db.setTransactionSuccessful();
		} finally {
			stmt.close();
			db.endTransaction();
			num_neighbours = -1;
		}

	}

	public void setNumMerged(final int num_merged) {
		this.num_merged = num_merged;
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET num_merged=? WHERE location_id=?", new String[] {
				Integer.toString(num_merged), Integer.toString(location_id) });
	}

	public void setTimestamp(final double timestamp) {
		this.timestamp = timestamp;
		// final ContentValues values = new ContentValues();
		// values.put("timestamp", "strftime(" + LocationSet.SQLITE_DATE_FORMAT
		// + "," + timestamp + ",'unixepoch')");
		// final String[] whereArgs = { Integer.toString(getId()) };
		// db.update(LocationSet.LOCATIONS_TABLE, values, "location_id=?",
		// whereArgs);
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET timestamp=strftime(" + LocationSet.SQLITE_DATE_FORMAT
				+ ",?,'unixepoch') " + "WHERE location_id=?", new Object[] {
				timestamp, getId() });
	}
}