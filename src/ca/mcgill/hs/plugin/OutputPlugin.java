package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public interface OutputPlugin extends Plugin{
	
	public boolean connect(DataInputStream dis);

}
