package ca.mcgill.hs.graph;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MagnitudeGraph extends Activity {

	// These are the values for the magnitude of the field.
	private static float[] magValues = null;

	// This is the first timestamp, the START
	private static long start = -1;

	// This is the second timestamp, the END
	private static long end = -1;

	// MagnitudeGraphView
	private MagnitudeGraphView mgv;

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
	 * @param values
	 *            The set of magnitude values required for this graph.
	 */
	protected static void setValues(final float[] values) {
		magValues = values;
	}

	/**
	 * This method is called when the activity is first created. This method is
	 * only called when data points are available.
	 */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
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

		mgv = new MagnitudeGraphView(this,
				"Please label your activities in this time period", magValues);

		setContentView(mgv);
	}

	/**
	 * This method gets called when this activity is no longer needed.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		magValues = null;
		start = -1;
		end = -1;
		finish();
	}

}
