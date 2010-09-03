package ca.mcgill.hs.classifiers.location;

import java.io.PrintStream;

public class DebugHelper {
	public static final boolean DEBUG = false;
	public static PrintStream out;
	static {
		if (DEBUG) {
			out = System.out;
		}
		else {
			out = new java.io.PrintStream (new java.io.OutputStream() {
				public void write (int b) {} 
			});
		}
	}
	
}
