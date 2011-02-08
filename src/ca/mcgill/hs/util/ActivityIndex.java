/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.util;

import java.io.Serializable;

/**
 * Represents activity/label pairs for the activity recognition stuff.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class ActivityIndex implements Serializable {
	private static final long serialVersionUID = -676311444086980081L;
	public final String[] activityNames;
	public final int[] activityCodes;

	public ActivityIndex(final String[] activityNames, final int[] activityCodes) {
		this.activityNames = activityNames;
		this.activityCodes = activityCodes;
	}
}
