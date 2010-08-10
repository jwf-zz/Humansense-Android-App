package ca.mcgill.hs.plugin;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.mcgill.hs.R;
import ca.mcgill.hs.plugin.BluetoothLogger.BluetoothPacket;
import ca.mcgill.hs.plugin.GPSLogger.GPSLoggerPacket;
import ca.mcgill.hs.plugin.GSMLogger.GSMLoggerPacket;
import ca.mcgill.hs.plugin.SensorLogger.SensorLoggerPacket;
import ca.mcgill.hs.plugin.WifiLogger.WifiLoggerPacket;
import ca.mcgill.hs.serv.UploaderService;
import ca.mcgill.hs.util.PreferenceFactory;

/**
 * An OutputPlugin which writes data to files.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 * 
 */
public class FileOutput extends OutputPlugin {

	// The preference manager for this plugin.
	final SharedPreferences prefs;

	// HashMap used for keeping file handles. There is one file associated with
	// each input plugin connected.
	private static final HashMap<Integer, DataOutputStream> fileHandles = new HashMap<Integer, DataOutputStream>();

	// File Extensions to be added at the end of each file.
	private static final String WIFI_EXT = "-wifiloc.log";
	private static final String GPS_EXT = "-gpsloc.log";
	private static final String SENS_EXT = "-raw.log";
	private static final String GSM_EXT = "-gsmloc.log";
	private static final String BT_EXT = "-bt.log";
	private static final String DEF_EXT = ".log";

	// Size of BufferedOutputStream buffer
	private static int BUFFER_SIZE;
	private static final String BUFFER_SIZE_KEY = "fileOutputBufferSize";

	// Rollover Interval pref key
	private final static String ROLLOVER_INTERVAL_KEY = "fileOutputRolloverInterval";

	// Boolean representing whether or not the plugin has been signalled to
	// stop.
	private Boolean PLUGIN_STOPPING;

	// Semaphore counter for the number of threads currently executing data
	// read/write operations.
	private int THREADS_WRITING;

	// Boolean ON-OFF switch *Temporary only*
	private boolean PLUGIN_ACTIVE;

	// A boolean making sure we're not uselessly uploading.
	private boolean hasRunOnce = false;

	// Preference key for this plugin's state
	private final static String PLUGIN_ACTIVE_KEY = "fileOutputEnabled";
	// Date format used in the log file names
	private final static String LOG_DATE_FORMAT = "yy-MM-dd-HHmmss";

	// Timestamps used for file rollover.
	private long initialTimestamp = -1;
	private long rolloverTimestamp = -1;
	private long ROLLOVER_INTERVAL;
	private long currentTimeMillis;

	// The Context in which to use preferences.
	private final Context context;

	/**
	 * This is the basic constructor for the FileOutput plugidatan. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a Context.
	 * 
	 * @param context
	 *            - the context in which this plugin is created.
	 */
	public FileOutput(final Context context) {
		this.context = context;
		PLUGIN_STOPPING = false;
		THREADS_WRITING = 0;

		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		PLUGIN_ACTIVE = prefs.getBoolean(PLUGIN_ACTIVE_KEY, false);
		BUFFER_SIZE = Integer.parseInt(prefs.getString(BUFFER_SIZE_KEY,
				(String) context.getResources().getText(
						R.string.fileoutput_buffersizedefault_pref)));

		ROLLOVER_INTERVAL = Integer.parseInt(prefs.getString(
				ROLLOVER_INTERVAL_KEY, (String) context.getResources().getText(
						R.string.fileoutput_rolloverintervaldefault_pref)));
	}

	/**
	 * Returns the list of Preference objects for this OutputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[3];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				PLUGIN_ACTIVE_KEY, R.string.fileoutput_pluginname_pref,
				R.string.fileoutput_pluginsummary_pref,
				R.string.fileoutput_pluginenabled_pref,
				R.string.fileoutput_plugindisabled_pref);
		prefs[1] = PreferenceFactory.getListPreference(c,
				R.array.fileOutputPluginBufferSizeStrings,
				R.array.fileOutputPluginBufferSizeValues, c.getResources()
						.getText(R.string.fileoutput_buffersizedefault_pref),
				BUFFER_SIZE_KEY, R.string.fileoutput_buffersize_pref,
				R.string.fileoutput_buffersize_pref_summary);
		prefs[2] = PreferenceFactory.getListPreference(c,
				R.array.fileOutputPluginRolloverIntervalStrings,
				R.array.fileOutputPluginRolloverIntervalValues,
				c.getResources().getText(
						R.string.fileoutput_rolloverintervaldefault_pref),
				ROLLOVER_INTERVAL_KEY,
				R.string.fileoutput_rolloverinterval_pref,
				R.string.fileoutput_rolloverinterval_pref_summary);

		return prefs;
	}

	/**
	 * Returns whether or not this OutputPlugin has Preferences.
	 * 
	 * @return whether or not this OutputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	/**
	 * Closes all open file handles.
	 */
	private void closeAll() {
		for (final Iterator<Integer> it = fileHandles.keySet().iterator(); it
				.hasNext();) {
			try {
				final int id = it.next();
				fileHandles.get(id).close();
				it.remove();
			} catch (final IOException e) {
				Log.e("FileOutput", "Caught IOException");
				e.printStackTrace();
			}
		}

		// In this block, we move all files that are in live (the most recent
		// files) into the recent directory.
		try {
			// Current live directory
			final File directory = new File(Environment
					.getExternalStorageDirectory(), (String) context
					.getResources().getText(R.string.live_file_path));
			if (!directory.isDirectory()) {
				if (!directory.mkdirs()) {
					throw new IOException("ERROR: Unable to create directory "
							+ directory.getName());
				}
			}

			// Get files in directory.
			final File[] filesInDirectory = directory.listFiles();

			// If directory not empty
			if (filesInDirectory != null) {

				// Destination directory
				final File dest = new File(Environment
						.getExternalStorageDirectory(), (String) context
						.getResources().getText(R.string.recent_file_path));
				if (!dest.isDirectory()) {
					if (!dest.mkdirs()) {
						throw new IOException(
								"ERROR: Unable to create directory "
										+ dest.getName());
					}
				}

				// Move files
				for (final File f : filesInDirectory) {
					if (!f.renameTo(new File(dest, f.getName()))) {
						throw new IOException("ERROR: Unable to transfer file "
								+ f.getName());
					}
				}
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}

		if (prefs.getBoolean("autoUploadData", false)) {
			Log.i("REACH", "REACH");
			final Intent uploadIntent = new Intent(context,
					UploaderService.class);
			context.startService(uploadIntent);
		}
	}

	/**
	 * Parses and writes given BluetoothPacket to given DataOutputStream.
	 * 
	 * @param gsmlp
	 *            the BluetoothPacket to parse and write out.
	 * @param dos
	 *            the DataOutputStream to write to.
	 */
	private void dataParse(final BluetoothPacket btp, final DataOutputStream dos) {
		try {
			dos.writeLong(btp.time);
			dos.writeInt(btp.neighbours);
			for (int i = 0; i < btp.neighbours; i++) {
				dos.writeUTF(btp.names.get(i) == null ? "null" : btp.names
						.get(i));
				dos.writeUTF(btp.addresses.get(i) == null ? "null"
						: btp.addresses.get(i));
			}
		} catch (final IOException e) {
			Log.e("FileOutput", "Caught IOException (BluetoothPacket parsing)");
			e.printStackTrace();
		}
	}

	/**
	 * Parses and writes given GPSLoggerPacket to given DataOutputStream.
	 * 
	 * @param gpslp
	 *            the GPSLoggerPacket to parse and write out.
	 * @param dos
	 *            the DataOutputStream to write to.
	 */
	private void dataParse(final GPSLoggerPacket gpslp,
			final DataOutputStream dos) {
		try {
			dos.writeLong(gpslp.time);
			dos.writeFloat(gpslp.accuracy);
			dos.writeFloat(gpslp.bearing);
			dos.writeFloat(gpslp.speed);
			dos.writeDouble(gpslp.altitude);
			dos.writeDouble(gpslp.latitude);
			dos.writeDouble(gpslp.longitude);
		} catch (final IOException e) {
			Log.e("FileOutput",
					"Caught IOException (GPSLocationPacket parsing)");
			e.printStackTrace();
		}
	}

	/**
	 * Parses and writes given GSMLoggerPacket to given DataOutputStream.
	 * 
	 * @param gsmlp
	 *            the GSMLoggerPacket to parse and write out.
	 * @param dos
	 *            the DataOutputStream to write to.
	 */
	private void dataParse(final GSMLoggerPacket gsmlp,
			final DataOutputStream dos) {
		try {
			dos.writeLong(gsmlp.time);
			dos.writeInt(gsmlp.mcc);
			dos.writeInt(gsmlp.mnc);
			dos.writeInt(gsmlp.cid);
			dos.writeInt(gsmlp.lac);
			dos.writeInt(gsmlp.rssi);
			dos.writeInt(gsmlp.neighbors);
			for (int i = gsmlp.neighbors - 1; i >= 0; i--) {
				dos.writeInt(gsmlp.cids[i]);
				dos.writeInt(gsmlp.lacs[i]);
				dos.writeInt(gsmlp.rssis[i]);
			}
		} catch (final IOException e) {
			Log.e("FileOutput", "Caught IOException (GSMLoggerPacket parsing)");
			e.printStackTrace();
		}
	}

	/**
	 * Parses and writes given SensorLoggerPacket to given DataOutputStream.
	 * 
	 * @param wlp
	 *            the WifiLoggerPacket to parse and write out.
	 * @param dos
	 *            the DataOutputStream to write to.
	 */
	private void dataParse(final SensorLoggerPacket slp,
			final DataOutputStream dos) {
		try {
			dos.writeLong(slp.time);
			dos.writeFloat(slp.x);
			dos.writeFloat(slp.y);
			dos.writeFloat(slp.z);
			dos.writeFloat(slp.m);
			dos.writeFloat(slp.temperature);
			for (final float f : slp.magfield) {
				dos.writeFloat(f);
			}
			for (final float f : slp.orientation) {
				dos.writeFloat(f);
			}
		} catch (final IOException e) {
			Log
					.e("FileOutput",
							"Caught IOException (WifiLoggerPacket parsing)");
			e.printStackTrace();
		}
	}

	/**
	 * Parses and writes given WifiLoggerPacket to given DataOutputStream.
	 * 
	 * @param wlp
	 *            the WifiLoggerPacket to parse and write out.
	 * @param dos
	 *            the DataOutputStream to write to.
	 */
	private void dataParse(final WifiLoggerPacket wlp,
			final DataOutputStream dos) {
		try {
			dos.writeInt(wlp.neighbors);
			dos.writeLong(wlp.timestamp);
			for (int i = wlp.neighbors - 1; i >= 0; i--) {
				dos.writeInt(wlp.levels[i]);
				dos.writeUTF(wlp.SSIDs[i]);
				dos.writeUTF(wlp.BSSIDs[i]);
			}
		} catch (final IOException e) {
			Log
					.e("FileOutput",
							"Caught IOException (WifiLoggerPacket parsing)");
			e.printStackTrace();
		}
	}

	/**
	 * Returns the String corresponding to the file extension (of the
	 * DataPacket) that should be added to the name of the file currently being
	 * created.
	 * 
	 * @param dp
	 *            the given DataPacket
	 * 
	 * @return the String representing the extension to add to the filename.
	 */
	private String getFileExtension(final DataPacket dp) {
		if (dp.getClass() == WifiLoggerPacket.class) {
			return WIFI_EXT;
		} else if (dp.getClass() == GPSLoggerPacket.class) {
			return GPS_EXT;
		} else if (dp.getClass() == SensorLoggerPacket.class) {
			return SENS_EXT;
		} else if (dp.getClass() == GSMLoggerPacket.class) {
			return GSM_EXT;
		} else if (dp.getClass() == BluetoothPacket.class) {
			return BT_EXT;
		} else {
			return DEF_EXT;
		}
	}

	/**
	 * This method gets called whenever an InputPlugin registered to FileOutput
	 * has data available to output. This method creates a file handle (if it
	 * doesn't exist already) for the InputPlugin the received DataPacket comes
	 * from. This method calls the appropriate version of dataParse based on the
	 * DataPacket type.
	 * 
	 * @param dp
	 *            the DataPacket received.
	 */
	@Override
	void onDataReceived(final DataPacket dp) {
		if (!PLUGIN_ACTIVE || PLUGIN_STOPPING) {
			return;
		}

		THREADS_WRITING++;
		final int id = dp.getDataPacketId();

		// Record system time
		currentTimeMillis = System.currentTimeMillis();

		// Check to see if files need to be rolled over
		if (currentTimeMillis >= rolloverTimestamp && ROLLOVER_INTERVAL != -1) {
			initialTimestamp = currentTimeMillis;

			// If files need to be rolled over, close all currently open
			// files and clear the hash map.
			if (hasRunOnce) {
				closeAll();
			} else {
				hasRunOnce = true;
			}
			Log.i("ROLLOVER", "Creating rollover timestamp.");
			rolloverTimestamp = currentTimeMillis + ROLLOVER_INTERVAL;
		}

		synchronized (fileHandles) {
			try {
				if (!fileHandles.containsKey(id)) {
					final File j = new File(Environment
							.getExternalStorageDirectory(), (String) context
							.getResources().getText(R.string.live_file_path));
					if (!j.isDirectory()) {
						if (!j.mkdirs()) {
							Log.e("Output Dir",
									"Could not create output directory!");
							return;
						}
					}

					// Generate file name based on the plugin it came from and
					// the current time.
					final Date d = new Date(currentTimeMillis);
					final SimpleDateFormat dfm = new SimpleDateFormat(
							LOG_DATE_FORMAT);
					final File fh = new File(j, dfm.format(d)
							+ getFileExtension(dp));
					if (!fh.exists()) {
						fh.createNewFile();
					}
					Log.i("File Output", "File to write: " + fh.getName());
					fileHandles.put(id, new DataOutputStream(
							new BufferedOutputStream(new GZIPOutputStream(
									new FileOutputStream(fh), BUFFER_SIZE))));
				}
			} catch (final IOException e) {
				Log.e("FileOutput", "Caught IOException");
				e.printStackTrace();
			}
		}

		// Choose correct dataParse method based on the format of the data
		// received.
		final DataOutputStream dos = fileHandles.get(id);
		if (id == SensorLoggerPacket.PLUGIN_ID) {
			dataParse((SensorLoggerPacket) dp, dos);
		} else if (id == WifiLoggerPacket.PLUGIN_ID) {
			dataParse((WifiLoggerPacket) dp, dos);
		} else if (id == GSMLoggerPacket.PLUGIN_ID) {
			dataParse((GSMLoggerPacket) dp, dos);
		} else if (id == GPSLoggerPacket.PLUGIN_ID) {
			dataParse((GPSLoggerPacket) dp, dos);
		} else if (id == SensorLoggerPacket.PLUGIN_ID) {
			dataParse((SensorLoggerPacket) dp, dos);
		} else if (id == BluetoothPacket.PLUGIN_ID) {
			dataParse((BluetoothPacket) dp, dos);
		}
		THREADS_WRITING--;
	}

	/**
	 * Closes all files currently open.
	 */
	@Override
	protected void onPluginStop() {
		PLUGIN_STOPPING = true;

		// Wait until all threads have finished writing.
		while (THREADS_WRITING != 0) {
		}

		closeAll();
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean(PLUGIN_ACTIVE_KEY,
				false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}

		ROLLOVER_INTERVAL = Integer.parseInt(prefs.getString(
				ROLLOVER_INTERVAL_KEY, (String) context.getResources().getText(
						R.string.fileoutput_rolloverintervaldefault_pref)));
		rolloverTimestamp = initialTimestamp + ROLLOVER_INTERVAL;

		BUFFER_SIZE = Integer.parseInt(prefs.getString(BUFFER_SIZE_KEY,
				(String) context.getResources().getText(
						R.string.fileoutput_buffersizedefault_pref)));
	}

}
