package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
import java.util.LinkedList;

/**
 * A simple interface detailing the behaviour of data collecting plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
	
	protected final LinkedList<DataOutputStream> dosList = new LinkedList<DataOutputStream>();
	
	public boolean connect(DataOutputStream dos) {
		return dosList.add(dos);
	}

}
