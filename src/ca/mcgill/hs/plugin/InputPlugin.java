package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
import java.util.LinkedList;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for using InputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
	
	//List of DataOutputStreams that the plugin will write to.
	protected final LinkedList<DataOutputStream> dosList = new LinkedList<DataOutputStream>();
	
	/**
	 * Adds the given DataOutputStream to the list of DataOutputStreams that the plugin will write to.
	 * @param dos the DataOutputStream to add to the list.
	 */
	public void connect(DataOutputStream dos) {
		dosList.add(dos);
	}

}
