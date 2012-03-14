package ca.mcgill.hs.plugin;

import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ca.mcgill.hs.util.Log;

public class LocationLogger extends InputPlugin {

	public static class LocationLabelDictionary extends SQLiteOpenHelper {

		public static final String TAG = "LocationLabelDictionary";
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "location-labels.db";
		private static final String DICTIONARY_TABLE_NAME = "labels";
		public static final String LABEL_COLUMN = "label";
		private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE "
				+ DICTIONARY_TABLE_NAME + " (" + LABEL_COLUMN
				+ " TEXT PRIMARY KEY);";

		public LocationLabelDictionary(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public LinkedList<String> getLabels() {
			final SQLiteDatabase db = getReadableDatabase();
			final LinkedList<String> labels = new LinkedList<String>();
			final Cursor cursor = db.query(DICTIONARY_TABLE_NAME,
					new String[] { LABEL_COLUMN }, null, null, null, null,
					LABEL_COLUMN);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				labels.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
			return labels;
		}

		public void insertLabel(final String label) {
			final SQLiteDatabase db = getWritableDatabase();
			final ContentValues values = new ContentValues();
			values.put(LABEL_COLUMN, label);
			Log.d(TAG, "Adding Location labeled " + label);
			db.insertWithOnConflict(DICTIONARY_TABLE_NAME, null, values,
					SQLiteDatabase.CONFLICT_IGNORE);
			db.close();
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(DICTIONARY_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			// Nothing to do yet.
		}
	}

	// TODO use android.support.v4.content.LocalBroadcastManager
	private class LocationLabeledReceiver extends BroadcastReceiver {
		private final LocationLabelDictionary db;
		private final LocationLogger logger;

		public LocationLabeledReceiver(final LocationLabelDictionary db,
				final LocationLogger logger) {
			this.db = db;
			this.logger = logger;
		}

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String label = intent
					.getStringExtra(LocationLabelDictionary.LABEL_COLUMN);
			Log.d(PLUGIN_NAME, "onReceive called with label: " + label);
			db.insertLabel(label);
			logger.write(new LocationPacket(System.currentTimeMillis(), label));
		}
	}

	public final static class LocationPacket implements DataPacket {
		final long time;
		final String location;

		final static String PACKET_NAME = "LocationPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

		public LocationPacket(final long time, final String location) {
			this.time = time;
			this.location = location;
		}

		@Override
		public DataPacket clone() {
			return new LocationPacket(time, location);
		}

		@Override
		public int getDataPacketId() {
			return LocationPacket.PACKET_ID;
		}

		@Override
		public String getInputPluginName() {
			return LocationLogger.PLUGIN_NAME;
		}
	}

	public static final String LOCATION_LABELED_ACTION = "ca.mcgill.hs.plugin.LocationLogger.LOCATION_LABELED";

	public final static String PLUGIN_NAME = "LocationLogger";

	public final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	/**
	 * @see InputPlugin#hasPreferences()
	 */
	public static boolean hasPreferences() {
		return false;
	}

	private final LocationLabelDictionary labelsDB;
	private final LocationLabeledReceiver receiver;
	private final Context context;

	public LocationLogger(final Context context) {
		labelsDB = new LocationLabelDictionary(context);
		receiver = new LocationLabeledReceiver(labelsDB, this);
		this.context = context;
	}

	@Override
	protected void onPluginStart() {
		Log.d(PLUGIN_NAME, "onPluginStart()");
		context.registerReceiver(receiver, new IntentFilter(
				LOCATION_LABELED_ACTION));
	}

	@Override
	protected void onPluginStop() {
		Log.d(PLUGIN_NAME, "onPluginStop()");
		context.unregisterReceiver(receiver);
	}
}
