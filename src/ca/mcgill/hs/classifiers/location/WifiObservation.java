package ca.mcgill.hs.classifiers.location;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WifiObservation extends Observation {
	protected final HashMap<Integer, Integer> measurements;
	protected int num_observations;
	protected double timestamp;

	// ETA is the percentage of aps that must be shared for two
	// observations to have a finite distance between them.
	protected static final float ETA = 0.3f;

	// Everything is more efficient if size is the true number of measurements,
	// but things will still work fine if it isn't.
	public WifiObservation(final double timestamp, final int size) {
		this.timestamp = timestamp;
		measurements = new HashMap<Integer, Integer>((int) (size / 0.75f));
		num_observations = 0;
	}

	public void addObservation(final int ap_id, final int signal_strength) {
		measurements.put(ap_id, signal_strength);
		num_observations += 1;
	}

	@Override
	public double distanceFrom(final Observation other) {
		final WifiObservation observation = (WifiObservation) other;

		// p1 will be the observation with the fewest measurements, to
		// speed up the distance calculation by having fewer iterations in
		// distance calculation loop.
		WifiObservation p1, p2;
		int num_common = 0; // |C| in the paper
		double dist = 0.0f;
		if (this.num_observations <= observation.num_observations) {
			p1 = this;
			p2 = observation;
		} else {
			p1 = observation;
			p2 = this;
		}

		Integer strength1;
		Integer strength2;

		for (final Map.Entry<Integer, Integer> entry : p1.measurements
				.entrySet()) {
			strength1 = p2.measurements.get(entry.getKey());
			if (strength1 != null) {
				strength2 = entry.getValue();
				dist += (strength1 - strength2) * (strength1 - strength2);
				num_common += 1;
			}
		}
		if ((float) num_common / (float) p2.num_observations > ETA) {
			dist = Math.sqrt((1.0f / num_common) * dist);
		} else {
			dist = Double.POSITIVE_INFINITY;
		}
		return dist;
	}

	@Override
	public double getEPS() {
		// TODO Auto-generated method stub
		return 6.0;
	}

	public double getTimeStamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Entry<Integer, Integer> entry : measurements.entrySet()) {
			sb.append("\tWapId: " + entry.getKey() + ", Strength: "
					+ entry.getValue() + "\n");
		}
		return sb.toString();

	}
}
