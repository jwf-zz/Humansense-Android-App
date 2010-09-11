package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class Location {

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
		db.execSQL("INSERT OR IGNORE INTO " + LocationSet.NEIGHBOURS_TABLE
				+ " VALUES (" + getId() + "," + id + ");");
	}

	/**
	 * Adds a set of neighbours to the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void addNeighbours(final Collection<Integer> ids) {
		for (final int id : ids) {
			if (id == getId()) {
				continue;
			}
			db.execSQL("INSERT OR IGNORE INTO " + LocationSet.NEIGHBOURS_TABLE
					+ " VALUES (" + getId() + "," + id + ");");
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
		final Cursor cursor = db.rawQuery("SELECT location_id2 FROM "
				+ LocationSet.NEIGHBOURS_TABLE + " WHERE location_id1="
				+ getId() + ";", null);
		while (cursor.moveToNext()) {
			neighbours.add(cursor.getInt(0));
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
			final Cursor cursor = db
					.rawQuery(
							"SELECT strftime('%s',timestamp,'localtime')-strftime('%S',timestamp,'localtime')+strftime('%f',timestamp,'localtime') FROM "
									+ LocationSet.LOCATIONS_TABLE
									+ " WHERE location_id=" + getId(), null);
			cursor.moveToNext();
			return cursor.getDouble(0);
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
		db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=" + getId() + " AND location_id2=" + id
				+ ";");
	}

	/**
	 * Removes a set of neighbours from the point.
	 * 
	 * @param points
	 *            A set of the point's neighbours.
	 */
	public void removeNeighbours(final Collection<Integer> neighboursToRemove) {
		for (final int id : neighboursToRemove) {
			db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
					+ " WHERE location_id1=" + getId() + " AND location_id2="
					+ id + ";");
		}
	}

	public void setTimestamp(final double timestamp) {
		this.timestamp = timestamp;
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET timestamp=strftime(" + LocationSet.SQLITE_DATE_FORMAT
				+ "," + timestamp + ",'unixepoch','localtime') "
				+ "WHERE location_id=" + getId() + ";");
	}
}
