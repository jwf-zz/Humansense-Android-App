/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import ca.mcgill.hs.util.Log;

/**
 * Represents a location, not necessarily physical, but characterized by a set
 * of observations, and a means for computing distances based on these
 * observations. Locations are stored in a database, tagged with a timestamp,
 * and maintain a list of neighbours.
 */
public abstract class Location {

	private static final String TAG = "Location";
	/**
	 * Locations can be merged, this keeps track of how many locations have been
	 * merged to create this current one
	 **/
	private long num_merged = -1;
	private long location_id = -1;

	protected SQLiteDatabase db = null;
	protected double timestamp = -1;
	private long num_neighbours = -1;
	private List<Long> neighbours = null;

	/**
	 * Creates a new location with the specified timestamp, and adds it to the
	 * database
	 * 
	 * @param db
	 *            The database in which these locations will be stored.
	 * @param timestamp
	 *            The timestamp, in milliseconds since the epoch (similar to
	 *            what is returned by System.currentTimeMillis()) associated
	 *            with the location. Note that this timestamp is really only
	 *            useful for debugging, and may change when points are merged.
	 * @throws SQLException
	 */
	public Location(final SQLiteDatabase db, final double timestamp) {
		this.db = db;
		this.location_id = generateNewLocationId();
		this.setTimestamp(timestamp);
		this.num_merged = 1;
		this.neighbours = new LinkedList<Long>();
	}

	/**
	 * Creates a location that is linked to a record in the database.
	 * 
	 * @param db
	 *            The database in which these locations will be stored.
	 * @param id
	 *            The id of the new location.
	 * @throws SQLException
	 */
	public Location(final SQLiteDatabase db, final long id) {
		this.location_id = id;
		this.db = db;
		this.setTimestamp(getTimestamp());
		this.num_merged = -1;
	}

	/**
	 * Adds a location as this location's neighbour.
	 * 
	 * @param id
	 *            The id of the neighbour to be added.
	 */
	public void addNeighbour(final long id) {
		if (id == getId()) {
			return;
		}
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		neighbours.add(id);
		db.execSQL("INSERT OR IGNORE INTO " + LocationSet.NEIGHBOURS_TABLE
				+ " VALUES (?,?)", new String[] { Long.toString(getId()),
				Long.toString(id) });
		num_neighbours = -1;
	}

	/**
	 * Adds a set of neighbours to the location.
	 * 
	 * @param ids
	 *            The ids of the neighbours to be added.
	 */
	public void addNeighbours(final Collection<Long> ids) {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		neighbours.addAll(ids);
		final SQLiteStatement stmt = db
				.compileStatement("INSERT OR IGNORE INTO "
						+ LocationSet.NEIGHBOURS_TABLE + " VALUES (?,?)");
		try {
			db.beginTransaction();
			for (final long id : ids) {
				if (id == getId()) {
					continue;
				}
				stmt.bindLong(1, getId());
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

	/**
	 * Calculates the distance between this and other.
	 * 
	 * @param other
	 *            The other location.
	 * @return The distance between this and other.
	 */
	public abstract double distanceFrom(Location other);

	/**
	 * Creates a new location record
	 * 
	 * @return The id of the new empty location that has been created.
	 */
	protected long generateNewLocationId() {
		// Insert Default Values
		final long new_id = db.insert(LocationSet.LOCATIONS_TABLE, "timestamp",
				null);
		return new_id;
	}

	public long getId() {
		return location_id;
	}

	/**
	 * Returns this location's neighbours.
	 * 
	 * @return This location's neighbours.
	 */
	public List<Long> getNeighbours() {
		if (neighbours == null) {
			final List<Long> neighbours = new LinkedList<Long>();
			num_neighbours = 0;
			final Cursor cursor = db.rawQuery(
					"SELECT location_id2,num_merged FROM "
							+ LocationSet.NEIGHBOURS_TABLE + " JOIN "
							+ LocationSet.LOCATIONS_TABLE
							+ " ON location_id=location_id2 "
							+ "WHERE location_id1=?", new String[] { Long
							.toString(getId()) });
			try {
				while (cursor.moveToNext()) {
					neighbours.add(cursor.getLong(0));
					num_neighbours += cursor.getLong(1);
				}
			} finally {
				cursor.close();
			}
			// Cache the neighbours
			this.neighbours = neighbours;
		}
		return neighbours;
	}

	public long getNumMerged() {
		if (num_merged > 0) {
			return num_merged;
		}
		final Cursor cursor = db.rawQuery("SELECT num_merged FROM "
				+ LocationSet.LOCATIONS_TABLE + " WHERE location_id=?",
				new String[] { Long.toString(getId()) });
		try {
			cursor.moveToNext();
			num_merged = cursor.getLong(0);
		} finally {
			cursor.close();
		}
		return num_merged;
	}

	public long getNumNeighbours() {
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
			num_neighbours = stmt.simpleQueryForLong();
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
							new String[] { Long.toString(getId()) });
			try {
				cursor.moveToNext();
				timestamp = cursor.getDouble(0);
			} catch (final CursorIndexOutOfBoundsException e) {
				Log.e(TAG, "UNABLE TO GET TIMESTAMP FOR LOCATION " + getId());
				Log.e(TAG, e);
			} finally {
				cursor.close();
			}
		}
		return timestamp;
	}

	/**
	 * Removes a location as this location's neighbour.
	 * 
	 * @param id
	 *            The id of the neighbouring location to be removed.
	 */
	public void removeNeighbour(final long id) {
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
				Long.toString(getId()), Long.toString(id) });
		// db.execSQL("DELETE FROM " + LocationSet.NEIGHBOURS_TABLE
		// + " WHERE location_id2=? AND location_id1=?", new String[] {
		// Long.toString(getId()), Long.toString(id) });
		num_neighbours = -1;
	}

	/**
	 * Removes a set of neighbours from the location.
	 * 
	 * @param neighboursToRemove
	 *            A set of the location's neighbours.
	 */
	public void removeNeighbours(final Collection<Long> neighboursToRemove) {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		final SQLiteStatement stmt = db.compileStatement("DELETE FROM "
				+ LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?");
		try {
			db.beginTransaction();
			for (final long id : neighboursToRemove) {
				try {
					neighbours.remove(id);
				} catch (final IndexOutOfBoundsException e) {
					// Ignore
				}
				stmt.bindLong(1, getId());
				stmt.bindLong(2, id);
				stmt.execute();
				stmt.bindLong(1, id);
				stmt.bindLong(2, getId());
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

	public void setNumMerged(final long l) {
		this.num_merged = l;
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET num_merged=? WHERE location_id=?", new String[] {
				Long.toString(l), Long.toString(location_id) });
	}

	public void setTimestamp(final double timestamp) {
		this.timestamp = timestamp;
		db.execSQL("UPDATE " + LocationSet.LOCATIONS_TABLE
				+ " SET timestamp=strftime(" + LocationSet.SQLITE_DATE_FORMAT
				+ ",?,'unixepoch') " + "WHERE location_id=?", new Object[] {
				timestamp, getId() });
	}
}
