package ca.mcgill.hs.plugin;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import ca.mcgill.hs.serv.HSService;

/**
 * Abstract class to be extended by all InputPlugins. Provides an interface for using InputPlugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
	
	//List of DataOutputStreams that the plugin will write to.
	protected final LinkedList<ObjectOutputStream> oosList = new LinkedList<ObjectOutputStream>();
	
	/**
	 * Adds the given DataOutputStream to the list of DataOutputStreams that the plugin will write to.
	 * @param dos the DataOutputStream to add to the list.
	 */
	public void connect(ObjectOutputStream oos) {
		oosList.add(oos);
	}
	
	
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
