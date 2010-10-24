package ca.mcgill.hs.graph;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import ca.mcgill.hs.R;

public class MagnitudeGraph extends Activity {

	public static interface GraphClosedRunnable extends Runnable {
		public void setLabelData(String label, float[] data);
	}

	private static final String TAG = "MagnitudeGraph";
	// These are the values for the magnitude of the field.
	private static Float[] magValues = null;
	private static int[] magActivities = null;
	private static boolean showLegendButton = true;
	private static GraphClosedRunnable onGraphClosed = null;

	private static boolean closeAfterOneActivity = false;

	// This is the first timestamp, the START
	private static long start = -1;

	// This is the second timestamp, the END
	private static long end = -1;

	public static void disableCloseAfterOneActivity() {
		closeAfterOneActivity = false;
	}

	public static void disableLegend() {
		showLegendButton = false;
	}

	public static void enableCloseAfterOneActivity() {
		closeAfterOneActivity = true;
	}

	public static void enableLegend() {
		showLegendButton = true;
	}

	/**
	 * This method sets the END timestamp. Both the START and END timestamps
	 * have to be set for this activity to start properly.
	 * 
	 * @param timestamp
	 *            The END timestamp.
	 */
	public static void setEndTimestamp(final long timestamp) {
		end = timestamp;
	}

	public static void setOnGraphClosed(final GraphClosedRunnable runnable) {
		onGraphClosed = runnable;
	}

	/**
	 * This method sets the START timestamp. Both the START and END timestamps
	 * have to be set for this activity to start properly.
	 * 
	 * @param timestamp
	 *            The START timestamp.
	 */
	public static void setStartTimestamp(final long timestamp) {
		start = timestamp;
	}

	/**
	 * This method sets up the array of floats for the magnitude values. This
	 * method has to be called before the activity starts or else it doesn't
	 * start.
	 * 
	 * @param magValues2
	 *            The set of magnitude values required for this graph.
	 */
	public static void setValues(final Float[] magValues2,
			final int[] activities) {
		magValues = magValues2;
		magActivities = activities;
	}

	// MagnitudeGraphView
	private MagnitudeGraphView mgv;

	/**
	 * This method is called when the activity is first created. This method is
	 * only called when data points are available.
	 */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "onCreate()");
		if (magValues == null || start == -1 || end == -1 || start == end) {
			Log
					.e(
							"MagniudeGraph",
							"Could not start Activity: one or more required values have not been previously set.");
			if (magValues == null) {
				Log.e("NotificationLauncher", "magValues was null;");
			}
			if (start == -1) {
				Log.e("NotificationLauncher", "start was not set;");
			}
			if (end == -1) {
				Log.e("NotificationLauncher", "end was not set;");
			}
			if (start == end) {
				Log.e("NotificationLauncher",
						"The start and end timestamps were the same;");
			}
			onDestroy();
		}

		mgv = new MagnitudeGraphView(this, getResources().getString(
				R.string.mag_graph_title), magValues, magActivities, start,
				end, showLegendButton, closeAfterOneActivity, onGraphClosed);
		onGraphClosed = null;
		setContentView(mgv);
	}

	/**
	 * This method gets called when this activity is no longer needed.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		magValues = null;
		start = -1;
		end = -1;
		finish();
	}
}
