/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Refers to an GPS location. Collections GPS observations to determine its
 * average location.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class GPSLocation extends Location {

	/** Epsilon from the paper, defines the neighbourhood size. */
	protected static final double EPS = 1E-5f;

	/**
	 * Creates a GPSLocation with the specified timestamp.
	 * 
	 * @param timestamp
	 *            The timestamp of the GPSLocation.
	 * @throws SQLException
	 *             Indicates there is some problem accessing the database.
	 * @see Location#Location(SQLiteDatabase, double)
	 */
	public GPSLocation(final SQLiteDatabase db, final double timestamp) {
		super(db, timestamp);
	}

	/**
	 * Creates a GPSLocation with the specified id.
	 * 
	 * @param db
	 * @param id
	 * @see Location#Location(SQLiteDatabase, long)
	 */
	public GPSLocation(final SQLiteDatabase db, final long id) {
		super(db, id);
	}

	/**
	 * Adds a new observation to the location.
	 * 
	 * @param o
	 *            The new {@link GPSObservation} to add to this location.
	 */
	@Override
	public void addObservation(final Observation o) {
		final GPSObservation observation = (GPSObservation) o;
		DebugHelper.out.println("\tObservation being added to location "
				+ getId());
		DebugHelper.out.println("\t" + observation.toString());

		db.execSQL("UPDATE " + GPSLocationSet.LOCATIONS_TABLE + " SET "
				+ "latitude_total=" + observation.latitude + ", "
				+ "latitude_weights=" + (1.0 / observation.accuracy) + ", "
				+ "longitude_total=" + observation.longitude + ", "
				+ "longitude_weights=" + (1.0 / observation.accuracy) + " "
				+ "WHERE location_id=" + getId() + ";");
	}

	@Override
	public double distanceFrom(final Location other) {
		/**
		 * The distance is Euclidean distance between the weighted means of the
		 * observations associated with each location, where the weights
		 * correspond to the accuracy of the GPS readings for the observations.
		 */
		final Cursor cursor = db.rawQuery(""
				+ "SELECT latitude_average,longitude_average FROM "
				+ GPSLocationSet.LOCATIONS_TABLE + " WHERE location_id="
				+ getId() + " OR location_id=" + other.getId() + ";", null);
		double lat1, lat2, lon1, lon2;
		try {
			cursor.moveToNext();
			lat1 = cursor.getDouble(0);
			lon1 = cursor.getDouble(1);
			cursor.moveToNext();
			lat2 = cursor.getDouble(0);
			lon2 = cursor.getDouble(1);
		} finally {
			cursor.close();
		}
		return Math.sqrt((lat1 - lat2) * (lat1 - lat2) + (lon1 - lon2)
				* (lon1 - lon2));
	}

	/**
	 * Returns the weighted average of the latitudes in the observations
	 * associated with this location. The weights correspond to the accuracies
	 * of the GPS observations.
	 * 
	 * @return The average latitude of the location.
	 */
	public double getLatitude() {
		double latitude = -1.0;
		final Cursor cursor = db.rawQuery("SELECT latitude_average FROM "
				+ WifiLocationSet.LOCATIONS_TABLE + " WHERE location_id="
				+ getId(), null);
		try {
			cursor.moveToNext();
			latitude = cursor.getDouble(0);
		} finally {
			cursor.close();
		}
		return latitude;
	}

	/**
	 * Returns the weighted average of the longitudes in the observations
	 * associated with this location. The weights correspond to the accuracies
	 * of the GPS observations.
	 * 
	 * @return The average longitude of the location.
	 */
	public double getLongitude() {
		double longitude = -1.0;
		final Cursor cursor = db.rawQuery("SELECT longitude_average FROM "
				+ GPSLocationSet.LOCATIONS_TABLE + " WHERE location_id="
				+ getId(), null);
		try {
			cursor.moveToNext();
			longitude = cursor.getDouble(0);
		} finally {
			cursor.close();
		}
		return longitude;
	}

	@Override
	public Observation getObservations() {
		Observation observation = null;
		final Cursor cursor = db.rawQuery("SELECT "
				+ "strftime('%s',timestamp) - " + "strftime('%S',timestamp) + "
				+ "strftime('%f',timestamp),"
				+ "latitude_weights, latitude_average, longitude_average "
				+ "FROM " + GPSLocationSet.LOCATIONS_TABLE
				+ " WHERE location_id=" + getId() + ";", null);
		try {
			cursor.moveToNext();
			observation = new GPSObservation(cursor.getDouble(0),
					(int) (1. / cursor.getDouble(1)), cursor.getDouble(2),
					cursor.getDouble(3));

		} finally {
			cursor.close();
		}
		return observation;
	}
}
