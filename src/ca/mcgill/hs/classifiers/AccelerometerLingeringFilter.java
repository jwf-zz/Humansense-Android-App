package ca.mcgill.hs.classifiers;

public class AccelerometerLingeringFilter {

	private static final String TAG = "AccelerometerLingeringFilter";
	private final float threshold;
	private final int windowSize;
	private int counter = 0;

	private float sum = 0.0f;
	private final float[] values;
	private boolean full = false;

	public AccelerometerLingeringFilter(final float threshold,
			final int windowSize) {
		this.threshold = threshold;
		this.windowSize = windowSize;
		values = new float[windowSize];
	}

	/**
	 * Adds a new value to the filter.
	 * 
	 * @param value
	 *            the new measurement to add to the window.
	 * @return true if the person is moving and false if the person is
	 *         stationary.
	 */
	public boolean update(float value) {
		if (full) {
			sum -= values[counter];
		}
		value = Math.abs(value);
		sum += value;
		values[counter] = value;
		counter += 1;
		if (counter >= windowSize) {
			full = true;
			counter = 0;
		}
		// Log.d(TAG, "Sum: " + sum);
		return sum / windowSize > threshold;
	}
}
