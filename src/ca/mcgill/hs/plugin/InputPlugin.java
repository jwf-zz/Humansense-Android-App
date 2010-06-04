package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for using InputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
	
	//List of ObjectOutputStreams that the plugin will write to.
	protected final LinkedList<ObjectOutputStream> oosList = new LinkedList<ObjectOutputStream>();
	
	/**
	 * Adds the given ObjectOutputStream to the list of ObjectOutputStreams that the plugin will write to.
	 * @param dos the ObjectOutputStream to add to the list.
	 */
	public void connect(ObjectOutputStream oos) {
		oosList.add(oos);
	}
	
	/**
	 * Writes the given DataPacket to each ObjectOutputStream connected to this plugin.
	 * @param dp the DataPacket to write.
	 */
	protected void write(DataPacket dp){
		try {
			for (ObjectOutputStream oos : oosList){
				oos.writeObject(dp);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
