package ca.mcgill.hs.classifiers.location;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class WifiLocation extends Location {

	/** Epsilon from the paper, defines the neighbourhood size. */
	public static final int EPS = 5;

	private int num_observations = -1;

	/**
	 * ETA is the percentage of WAPs that must be shared for two observations to
	 * have a finite distance between them.
	 */
	public static final double ETA = 0.5;

	private static final String TAG = "WifiLocation";

	/**
	 * Computes the distance between two observations.
	 * 
	 * @param observation
	 *            The second observation.
	 * @return The distance between two observations.
	 */

	private SQLiteStatement distStmt;

	public WifiLocation(final SQLiteDatabase db, final double timestamp) {
		super(db, timestamp);
	}

	/**
	 * A mapping from WAP IDs to the sums signal strengths associated with this
	 * observation.
	 */
	// private HashMap<Integer,Integer> wapStrengths = new
	// HashMap<Integer,Integer>();

	public WifiLocation(final SQLiteDatabase db, final int id) {
		super(db, id);
	}

	/**
	 * Adds a generic WiFi observation to this observation's list. The signal
	 * strength of the observation is added to this observation's mapping.
	 * 
	 * @param observation
	 *            The WiFi observation to be added.
	 */
	@Override
	public void addObservation(final Observation o) {
		final WifiObservation observation = (WifiObservation) o;
		// DebugHelper.out.println("Here with " + observation.num_observations +
		// " observations.");
		for (final Entry<Integer, Integer> entry : observation.measurements
				.entrySet()) {
			final int wap_id = entry.getKey();
			ensureObservationExists(wap_id);

		}

		for (final Entry<Integer, Integer> entry : observation.measurements
				.entrySet()) {
			final int strength = entry.getValue();
			final int wap_id = entry.getKey();
			updateStrength(wap_id, strength);
		}
		// Invalidate any cached distances
		invalidateDistances();
		updateNumObservations();
	}

	@Override
	public double distanceFrom(final Location other) {
		final WifiLocation location = (WifiLocation) other;
		double dist = 0.0;
		int num_common = 0;

		// Check to see if the distance has been cached
		final String[] params = { Integer.toString(this.getId()),
				Integer.toString(location.getId()) };
		Cursor cursor = db.rawQuery("SELECT distance FROM "
				+ LocationSet.NEIGHBOURS_TABLE
				+ " WHERE location_id1=? AND location_id2=?", params);
		try {
			cursor.moveToNext();
			dist = cursor.getDouble(0);
		} finally {
			cursor.close();
		}
		if (dist >= 0.0) {
			Log.d(TAG, "Caching works; Marvelous!!!");
			return dist;
		}
		// final Cursor cursor = db
		// .rawQuery(
		// "SELECT o1.average_strength,o2.average_strength FROM observations AS o1 "
		// + "JOIN observations AS o2 USING (wap_id) "
		// + "WHERE o1.location_id=" + this.getId()
		// + " AND o2.location_id=" + location.getId()
		// + ";", null);
		cursor = db
				.rawQuery(
						"SELECT o1.average_strength,o2.average_strength FROM "
								+ WifiLocationSet.OBSERVATIONS_TABLE
								+ " AS o1 " + "JOIN "
								+ WifiLocationSet.OBSERVATIONS_TABLE
								+ " AS o2 USING (wap_id) "
								+ "WHERE o1.location_id=? AND o2.location_id=?",
						params);
		try {
			while (cursor.moveToNext()) {
				final double part = cursor.getDouble(0) - cursor.getDouble(1);
				dist += part * part;
				num_common += 1;
			}
		} finally {
			cursor.close();
		}

		if ((double) num_common
				/ (double) Math.max(this.numObservations(), location
						.numObservations()) < ETA) {
			dist = Double.POSITIVE_INFINITY;
		} else {
			dist = Math.sqrt((1.0 / num_common) * dist);
			final Object[] params2 = { dist, this.getId(), location.getId() };
			// Log.d(TAG, "Executing: " + "UPDATE " +
			// LocationSet.NEIGHBOURS_TABLE
			// + " SET distance=" + params2[0] + " WHERE location_id1="
			// + params2[1] + " AND location_id2=" + params2[2]);
			db
					.execSQL(
							"UPDATE "
									+ LocationSet.NEIGHBOURS_TABLE
									+ " SET distance=? WHERE location_id1=? AND location_id2=?",
							params2);
		}
		return dist;
	}

	private void ensureObservationExists(final int wap_id) {
		final Object[] params = { getId(), wap_id };
		db.execSQL("INSERT OR IGNORE INTO "
				+ WifiLocationSet.OBSERVATIONS_TABLE + " VALUES (?,?,0,0,0)",
				params);
	}

	/**
	 * Returns the average strength of this observation at the specified WAP ID.
	 * 
	 * @param wapID
	 *            The WAP ID to examine.
	 * @return The average strength of this observation at the specified WAP ID.
	 */
	public double getAvgStrength(final int wap_id) {
		double avgStrength = 0.0;
		final String[] params = { Integer.toString(getId()),
				Integer.toString(wap_id) };
		final Cursor cursor = db.rawQuery("SELECT strength,count FROM "
				+ WifiLocationSet.OBSERVATIONS_TABLE
				+ " WHERE location_id=? AND wap_id=?", params);
		try {
			cursor.moveToNext();
			avgStrength = (double) cursor.getInt(0) / (double) cursor.getInt(1);
		} finally {
			cursor.close();
		}
		return avgStrength;
	}

	public Set<Integer> getObservableWAPs() {
		final Set<Integer> wap_ids = new HashSet<Integer>();
		final String[] params = { Integer.toString(getId()) };
		final Cursor cursor = db.rawQuery("SELECT wap_id FROM "
				+ WifiLocationSet.OBSERVATIONS_TABLE + " WHERE location_id=?",
				params);
		try {
			while (cursor.moveToNext()) {
				wap_ids.add(cursor.getInt(0));
			}
		} finally {
			cursor.close();
		}
		return wap_ids;
	}

	@Override
	public Observation getObservations() {
		final WifiObservation observation = new WifiObservation(getTimestamp(),
				35);
		final String[] params = { Integer.toString(getId()) };
		final Cursor cursor = db.rawQuery("SELECT wap_id,strength,count FROM "
				+ WifiLocationSet.OBSERVATIONS_TABLE + " WHERE location_id=?",
				params);
		try {
			while (cursor.moveToNext()) {
				observation.addObservation(cursor.getInt(0), cursor.getInt(1));
			}
		} finally {
			cursor.close();
		}
		return observation;
	}

	private void invalidateDistances() {
		final Object[] params = { getId() };
		db.execSQL("UPDATE " + LocationSet.NEIGHBOURS_TABLE
				+ " SET distance=-1.0 WHERE location_id1=?", params);
	}

	public int numObservations() {
		if (num_observations < 0) {
			updateNumObservations();
		}
		return num_observations;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Location Id: " + getId() + "\n");
		sb.append("\tWAPS:\n");
		final Set<Integer> wap_ids = getObservableWAPs();
		for (final int wap_id : wap_ids) {
			sb.append("\t\tId: " + wap_id + "\tAverage Strength: "
					+ getAvgStrength(wap_id) + "\n");
		}
		sb.append("\tNeighbours:");
		final List<Integer> neighbours = getNeighbours();
		for (final int neighbour : neighbours) {
			sb.append(" " + neighbour);
		}
		sb.append("\n");
		return sb.toString();
	}

	private void updateNumObservations() {
		final String[] params = { new Integer(getId()).toString() };
		final Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM "
				+ WifiLocationSet.OBSERVATIONS_TABLE + " WHERE location_id=?",
				params);
		try {
			cursor.moveToNext();
			num_observations = cursor.getInt(0);
		} finally {
			cursor.close();
		}
	}

	private void updateStrength(final int wap_id, final int strength) {
		final Object[] params = { strength, getId(), wap_id };
		db.execSQL("UPDATE " + WifiLocationSet.OBSERVATIONS_TABLE
				+ " SET strength=? WHERE location_id=? AND wap_id=?", params);
	}
}
