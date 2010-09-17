package ca.mcgill.hs.classifiers;

public class AccelerometerLingeringFilter {

	@SuppressWarnings("unused")
	private static final String TAG = "AccelerometerLingeringFilter";

	private float threshold;
	private int windowSize;

	private int counter = 0;

	private float sum = 0.0f;

	private float[] values;

	private boolean full = false;

	public AccelerometerLingeringFilter(final float threshold,
			final int windowSize) {
		this.threshold = threshold;
		this.windowSize = windowSize;
		values = new float[windowSize];
	}

	public float getThreshold() {
		return threshold;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setThreshold(final float threshold) {
		this.threshold = threshold;
	}

	public void setWindowSize(final int windowSize) {
		// If we change the size of the window, clear the values buffer.
		if (this.windowSize != windowSize) {
			values = new float[windowSize];
			counter = 0;
			full = false;
		}
		this.windowSize = windowSize;

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
