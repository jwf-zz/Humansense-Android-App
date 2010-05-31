package ca.mcgill.hs.plugin;

import java.io.DataOutputStream;
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
	protected final LinkedList<DataOutputStream> dosList = new LinkedList<DataOutputStream>();
	
	//List of methods to be called in order when writing data to the OutputStream.
	protected final LinkedList<Method> callStack = new LinkedList<Method>();
	
	private final int[] formatTypes = { HSService.STRING, HSService.INT, HSService.FLOAT, HSService.DOUBLE,
			HSService.LONG, HSService.SHORT, HSService.BYTE, HSService.CHAR, HSService.BOOLEAN, HSService.ARRAY};
	private final Class[] formatClasses = { String.class, Integer.TYPE, Float.TYPE, Double.TYPE, Long.TYPE,
			Short.TYPE, Byte.TYPE, Character.TYPE, Boolean.TYPE, Integer.TYPE};
	private final String[] formatCalls = { "writeUTF", "writeInt", "writeFloat", "writeDouble", "writeLong",
			"writeShort", "writeByte", "writeChar", "writeBoolean", "writeInt"};

	/**
	 * Adds the given DataOutputStream to the list of DataOutputStreams that the plugin will write to.
	 * @param dos the DataOutputStream to add to the list.
	 */
	public void connect(DataOutputStream dos) {
		dosList.add(dos);
	}
	
	/**
	 * Writes the dataResult object array to every DataOutputStream connected to this InputPlugin using
	 * the appropriate methods from the callStack.
	 * @param dataResult the Object array of data to be written.
	 */
	protected void write(Object[] dataResult){
		int i = 0;
		for (Method m : callStack){
			for (DataOutputStream dos : dosList){
				try {
					m.invoke(dos, dataResult[i]);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			i++;
		}
	}
	
	/**
	 * Searches the inputInfo for data formats and saves them into formatList, then passes formatList to
	 * setCallStack() to generate a Method call stack.
	 * Note that inputInfo contains other things besides Strings representing data types. It also contains
	 * the names of variables and read() Method calls for use in OutputPlugins, so the list is first cut up
	 * to remove everything except the format Strings.
	 * @param inputInfo the input info retrieved from the plugin's XML file.
	 */
	public void generateCallStack(LinkedList<Object> inputInfo){
		//The list of format Strings.
		LinkedList<Integer> formatList = new LinkedList<Integer>();
		
		//For details on exactly what is being removed and why see HSService.getInputInfo().
		//Remove first unneeded element (class name).
		inputInfo.remove();
		while (!inputInfo.isEmpty()){
			//Get the data format String.
			formatList.add((Integer) inputInfo.remove());
			//Remove the name String and the Method.
			inputInfo.remove();
			inputInfo.remove();
		}
		
		//Generate the call stack.
		setCallStack(formatList);
	}
	
	/**
	 * Populates the call stack list based on the formatList.
	 * @param formatList the Linked List of data formats that will be written to the output data stream.
	 */
	private void setCallStack(LinkedList<Integer> formatList){
		Class[] arg = new Class[1];
		boolean methodFound = false;
		for (int format : formatList){
			methodFound = false;
			try {
				for (int i = 0; i < formatTypes.length; i++){
					if (format == formatTypes[i]){
						arg[0] = formatClasses[i];
						callStack.add(DataOutputStream.class.getMethod(formatCalls[i], arg));
						methodFound = true;
						break;
					}
				}
				if (!methodFound) throw new NoSuchMethodException();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}

}
