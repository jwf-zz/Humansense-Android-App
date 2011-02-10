/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import ca.mcgill.hs.R;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

/**
 * Input plugin for logging GSM radio data.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public final class GSMLogger extends InputPlugin {

	/**
	 * Packet for transmitting GSM radio data.
	 * 
	 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
	 * 
	 */
	public static class GSMPacket implements DataPacket {
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
		final static String PACKET_NAME = "GSMPacket";
		final static int PACKET_ID = PACKET_NAME.hashCode();

		public GSMPacket(final long time, final int mcc, final int mnc,
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
			return new GSMPacket(time, mcc, mnc, cid, lac, rssi, neighbors,
					cids, lacs, rssis);
		}

		@Override
		public int getDataPacketId() {
			return GSMPacket.PACKET_ID;
		}

		@Override
		public String getInputPluginName() {
			return GSMLogger.PLUGIN_NAME;
		}

	}

	private static final String GSM_LOGGER_ENABLE_PREF = "gsmLoggerEnable";
	public final static String PLUGIN_NAME = "GSMLogger";
	public final static int PLUGIN_ID = PLUGIN_NAME.hashCode();

	/**
	 * @see InputPlugin#getPreferences(PreferenceActivity)
	 */
	public static Preference[] getPreferences(final PreferenceActivity activity) {
		final Preference[] prefs = new Preference[1];

		prefs[0] = PreferenceFactory.getCheckBoxPreference(activity,
				GSM_LOGGER_ENABLE_PREF, R.string.gsmlogger_enable_pref_label,
				R.string.gsmlogger_enable_pref_summary,
				R.string.gsmlogger_enable_pref_on,
				R.string.gsmlogger_enable_pref_off, false);

		return prefs;
	}

	/**
	 * @see InputPlugin#hasPreferences()
	 */
	public static boolean hasPreferences() {
		return true;
	}

	// The TelephonyManager required to receive information about the device.
	private final TelephonyManager telephonyManager;

	// A PhoneStateListener used to listen to phone signals.
	private PhoneStateListener phoneStateListener;

	private final SharedPreferences prefs;

	/**
	 * Constructs the plugin.
	 * 
	 * @param context
	 *            The application context, required to access the preferences
	 *            and manage the telephony hardware.
	 */
	public GSMLogger(final Context context) {
		telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		prefs = PreferenceFactory.getSharedPreferences(context);
	}

	@Override
	protected void onPluginStart() {
		pluginEnabled = prefs.getBoolean(GSM_LOGGER_ENABLE_PREF, false);
		if (!pluginEnabled) {
			return;
		}
		// Log.d(TAG, "Network Operator: " + tm.getNetworkOperator());
		// Log.d(TAG, "Network Operator Name: " + tm.getNetworkOperatorName());
		// Log.d(TAG, "Network Type: " + tm.getNetworkType());
		// Log.d(TAG, "Phone Type: " + tm.getPhoneType());
		if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM
				&& telephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
			phoneStateListener = new PhoneStateListener() {
				private int rssi = -1;
				private int mcc = -1;
				private int mnc = -1;

				public void logSignals(final GsmCellLocation cell) {
					if (cell == null) {
						return;
					}
					final long time = System.currentTimeMillis();
					final int cid = cell.getCid();
					final int lac = cell.getLac();

					final List<NeighboringCellInfo> neighbours = telephonyManager
							.getNeighboringCellInfo();

					final int ns = neighbours.size();
					final int[] cids = new int[ns];
					final int[] lacs = new int[ns];
					final int[] rssis = new int[ns];

					int asu;
					int i = 0;
					for (final NeighboringCellInfo neighbour : neighbours) {
						cids[i] = neighbour.getCid();
						lacs[i] = neighbour.getLac();
						asu = neighbour.getRssi();
						if (asu == -1) {
							rssis[i] = -1;
						} else {
							rssis[i] = (-113 + 2 * asu);
						}
						i++;
					}

					write(new GSMPacket(time, mcc, mnc, cid, lac, this.rssi,
							ns, cids, lacs, rssis));
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
							try {
								mcc = Integer.parseInt(op.substring(0, 3));
								mnc = Integer.parseInt(op.substring(3));
							} catch (final Exception e) {
							}
						}
						CellLocation.requestLocationUpdate();
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
					if (strength.isGsm()) {
						final int asu = strength.getGsmSignalStrength();
						if (asu == -1) {
							rssi = -1;
						} else {
							rssi = (-113 + 2 * asu);
						}
						logSignals((GsmCellLocation) telephonyManager
								.getCellLocation());
					} else {
						Log.d(PLUGIN_NAME, "Signal Strength not for GSM.");
					}
				}
			};
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_SERVICE_STATE
							| PhoneStateListener.LISTEN_CELL_LOCATION
							| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		} else {
			Log
					.i(PLUGIN_NAME,
							"GSM Location Logging Unavailable! Wrong phone type or SIM card not present!");
		}
	}

	@Override
	protected void onPluginStop() {
		if (!pluginEnabled) {
			return;
		}
		if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_NONE);
		}
	}

	@Override
	public void onPreferenceChanged() {
		final boolean pluginEnabledNew = prefs.getBoolean(
				GSM_LOGGER_ENABLE_PREF, false);
		super.changePluginEnabledStatus(pluginEnabledNew);
	}

}
