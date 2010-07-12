package ca.mcgill.hs.plugin;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import ca.mcgill.hs.util.PreferenceFactory;

public class GSMLogger extends InputPlugin {

	public static class GSMLoggerPacket implements DataPacket {
		final long time;
		final int mcc;
		final int mnc;
		final int cid;
		final int lac;
		final int rssi;
		final int neighbors;
		final int[] cids;
		final int[] lacs;
		final int[] rssis;
		final static String PLUGIN_NAME = "GSMLogger";
		final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

		public GSMLoggerPacket(final long time, final int mcc, final int mnc,
				final int cid, final int lac, final int rssi,
				final int neighbors, final int[] cids, final int[] lacs,
				final int[] rssis) {
			this.time = time;
			this.mcc = mcc;
			this.mnc = mnc;
			this.cid = cid;
			this.lac = lac;
			this.rssi = rssi;
			this.neighbors = neighbors;
			this.cids = cids;
			this.lacs = lacs;
			this.rssis = rssis;
		}

		@Override
		public DataPacket clone() {
			return new GSMLoggerPacket(time, mcc, mnc, cid, lac, rssi,
					neighbors, cids, lacs, rssis);
		}

		@Override
		public int getDataPacketId() {
			return GSMLoggerPacket.PLUGIN_ID;
		}

		@Override
		public String getInputPluginName() {
			return GSMLoggerPacket.PLUGIN_NAME;
		}

	}

	// Boolean ON-OFF switch *Temporary only*
	private boolean PLUGIN_ACTIVE;

	// A String used for logging information to the android's logcat.
	private static final String TAG = "GSMLogger";

	// The TelephonyManager required to receive information about the device.
	private final TelephonyManager tm;

	// A PhoneStateListener used to listen to phone signals.
	private PhoneStateListener psl;

	// Variables used to write out the GSM data received.
	private static long time;
	private static int cid;
	private static int lac;
	private static int ns;
	private static int[] cids;
	private static int[] lacs;

	private static int[] rssis;

	/**
	 * Returns the list of Preference objects for this InputPlugin.
	 * 
	 * @param c
	 *            the context for the generated Preferences.
	 * @return an array of the Preferences of this object.
	 */
	public static Preference[] getPreferences(final Context c) {
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(c,
				"gsmLoggerEnable", "GSM Plugin",
				"Enables or disables this plugin.", "GSMLogger is on.",
				"GSMLogger is off.");

		return prefs;
	}

	/**
	 * Returns whether or not this InputPlugin has Preferences.
	 * 
	 * @return whether or not this InputPlugin has preferences.
	 */
	public static boolean hasPreferences() {
		return true;
	}

	// The Context used for the plugins.
	private final Context context;

	/**
	 * This is the basic constructor for the GSMLogger plugin. It has to be
	 * instantiated before it is started, and needs to be passed a reference to
	 * a TelephonyManager and a Context.
	 * 
	 * @param wm
	 *            the WifiManager for this WifiLogger.
	 * @param context
	 *            the context in which this plugin is created.
	 */
	public GSMLogger(final TelephonyManager tm, final Context context) {
		this.tm = tm;
		this.context = context;

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		PLUGIN_ACTIVE = prefs.getBoolean("gsmLoggerEnable", false);
	}

	/**
	 * This method gets called whenever the preferences have been changed.
	 */
	@Override
	public void onPreferenceChanged() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		final boolean new_PLUGIN_ACTIVE = prefs.getBoolean("gsmLoggerEnable",
				false);
		if (PLUGIN_ACTIVE && !new_PLUGIN_ACTIVE) {
			stopPlugin();
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
		} else if (!PLUGIN_ACTIVE && new_PLUGIN_ACTIVE) {
			PLUGIN_ACTIVE = new_PLUGIN_ACTIVE;
			startPlugin();
		}
	}

	/**
	 * Taken from Jordan Frank
	 * (hsandroidv1.ca.mcgill.cs.humansense.hsandroid.service) and modified for
	 * this plugin.
	 */
	public void startPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM
				&& tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
			psl = new PhoneStateListener() {
				private int rssi = -1, mcc = -1, mnc = -1;

				public void logSignals(final GsmCellLocation cell) {
					time = System.currentTimeMillis();
					cid = cell.getCid();
					lac = cell.getLac();

					final List<NeighboringCellInfo> neighbours = tm
							.getNeighboringCellInfo();

					ns = neighbours.size();
					cids = new int[ns];
					lacs = new int[ns];
					rssis = new int[ns];

					int asu, rssi;
					int i = 0;
					for (final NeighboringCellInfo neighbour : neighbours) {
						cids[i] = neighbour.getCid();
						lacs[i] = neighbour.getLac();
						asu = neighbour.getRssi();
						if (asu == -1) {
							rssi = -1;
						} else {
							rssi = (-113 + 2 * asu);
						}
						rssis[i] = rssi;
						i++;
					}

					write(new GSMLoggerPacket(time, mcc, mnc, cid, lac,
							this.rssi, ns, cids, lacs, rssis));
				}

				@Override
				public void onCellLocationChanged(final CellLocation cell) {
					super.onCellLocationChanged(cell);
					logSignals((GsmCellLocation) cell);
				}

				@Override
				public void onServiceStateChanged(
						final ServiceState serviceState) {
					super.onServiceStateChanged(serviceState);
					final int state = serviceState.getState();
					switch (state) {
					case ServiceState.STATE_IN_SERVICE:
					case ServiceState.STATE_EMERGENCY_ONLY:
						final String op = serviceState.getOperatorNumeric();

						if (op.length() > 3) {
							final String mccStr = op.substring(0, 3);
							final String mncStr = op.substring(3);

							try {
								mcc = Integer.parseInt(mccStr);
								mnc = Integer.parseInt(mncStr);
							} catch (final Exception e) {
							}
						}

						break;
					case ServiceState.STATE_POWER_OFF:
						break;
					case ServiceState.STATE_OUT_OF_SERVICE:
						break;
					}

				}

				@Override
				public void onSignalStrengthsChanged(
						final SignalStrength strength) {
					super.onSignalStrengthsChanged(strength);
					final int asu = strength.getGsmSignalStrength();
					if (asu == -1) {
						rssi = -1;
					} else {
						rssi = (-113 + 2 * asu);
					}

					logSignals((GsmCellLocation) tm.getCellLocation());
				}
			};
			tm.listen(psl, PhoneStateListener.LISTEN_SERVICE_STATE
					| PhoneStateListener.LISTEN_CELL_LOCATION
					| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		} else {
			Log
					.i(TAG,
							"GSM Location Logging Unavailable! Wrong phone type or SIM card not present!");
		}
	}

	/**
	 * Tells the phone to stop listening for available GSM signals.
	 */
	public void stopPlugin() {
		if (!PLUGIN_ACTIVE) {
			return;
		}
		if (tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
			tm.listen(psl, PhoneStateListener.LISTEN_NONE);
		}
	}

}
