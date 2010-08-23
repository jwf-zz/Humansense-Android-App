package ca.mcgill.hs.util;

import java.io.Serializable;

public class ActivityIndex implements Serializable {
	private static final long serialVersionUID = -676311444086980081L;
	public final String[] activityNames;
	public final int[] activityCodes;

	public ActivityIndex(final String[] activityNames, final int[] activityCodes) {
		this.activityNames = activityNames;
		this.activityCodes = activityCodes;
	}
}
