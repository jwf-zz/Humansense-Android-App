/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers;

/**
 * Very simple filter for accelerometer values. Maintains a moving window of
 * accelerometer magnitude values and computes whether the average value is
 * greater than some threshold. Can act as a very simple test for motion.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 */
public class AccelerometerLingeringFilter {

	@SuppressWarnings("unused")
	private static final String TAG = "AccelerometerLingeringFilter";

	private float threshold;
	private int windowSize;

	private int counter = 0;

	private float sum = 0.0f;

	private float[] values;

	private boolean full = false;

	/**
	 * Creates a new lingering filter.
	 * 
	 * @param threshold
	 *            Threshold against which the average value of the buffer is
	 *            compared against.
	 * @param windowSize
	 *            The size of the rolling window.
	 */
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

	/**
	 * Sets the size of the rolling window. Note that changing the size will
	 * clear the existing values from the window.
	 * 
	 * @param windowSize
	 *            The size of the rolling window.
	 */
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
	 *            The new measurement to add to the window.
	 * @return True if the average of the measurements in the window are above
	 *         the threshold, and false otherwise.
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
		return sum / windowSize > threshold;
	}
}
