package ca.mcgill.hs.classifiers.location;

import java.sql.SQLException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Refers to an GPS location. Collections GPS observations to determine its
 * average location.
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
	 */
	public GPSLocation(final SQLiteDatabase db, final double timestamp) {
		super(db, timestamp);
	}

	public GPSLocation(final SQLiteDatabase db, final int id) {
		super(db, id);
	}

	@Override
	public void addObservation(final Observation o) {
		final GPSObservation observation = (GPSObservation) o;
		DebugHelper.out.println("\tObservation being added to location "
				+ getId());
		DebugHelper.out.println("\t" + observation.toString());

		// conn.createStatement().executeUpdate(
		// "INSERT OR IGNORE INTO " + GPSLocationSet.LOCATIONS_TABLE +
		// " VALUES (" +
		// getId() + ", " +
		// getTimestamp() + ", " +
		// "NULL,0,0,0,0,0,0,1);"
		// );
		db.execSQL("UPDATE " + GPSLocationSet.LOCATIONS_TABLE + " SET "
				+ "latitude_total=" + observation.latitude + ", "
				+ "latitude_weights=" + (1.0 / observation.accuracy) + ", "
				+ "longitude_total=" + observation.longitude + ", "
				+ "longitude_weights=" + (1.0 / observation.accuracy) + " "
				+ "WHERE location_id=" + getId() + ";");
	}

	@Override
	public double distanceFrom(final Location other) {
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
	 * Returns the average latitude of the location.
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
	 * Returns the average longitude of the location.
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
		final Cursor cursor = db
				.rawQuery(
						"SELECT strftime('%s',timestamp)-strftime('%S',timestamp)+strftime('%f',timestamp),latitude_weights,latitude_average,longitude_average FROM "
								+ GPSLocationSet.LOCATIONS_TABLE
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
