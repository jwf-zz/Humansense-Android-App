/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.io.PrintStream;

public class DebugHelper {
	public static final boolean DEBUG = false;
	public static PrintStream out;
	static {
		if (DEBUG) {
			out = System.out;
		} else {
			out = new java.io.PrintStream(new java.io.OutputStream() {
				@Override
				public void write(final int b) {
				}
			});
		}
	}

}
