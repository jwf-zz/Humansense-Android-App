package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * A simple interface detailing the behaviour of data collecting plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class InputPlugin implements Plugin{
	
	//List of DataOutputStreams that the plugin will write to.
	protected final LinkedList<DataOutputStream> dosList = new LinkedList<DataOutputStream>();
	
	protected final LinkedList<Method> callStack = new LinkedList<Method>();
	
	/**
	 * Adds the given DataOutputStream to the list of DataOutputStreams that the plugin will write to.
	 * @param dos the DataOutputStream to add to the list.
	 */
	public void connect(DataOutputStream dos) {
		dosList.add(dos);
	}
	
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
	
	public void generateCallStack(LinkedList<Object> inputInfo){
		//The list of format strings
		LinkedList<String> formatList = new LinkedList<String>();
		
		//Remove first unneeded element (class name)
		inputInfo.remove();
		while (!inputInfo.isEmpty()){
			//get format (what we want)
			formatList.add((String) inputInfo.remove());
			//remove the name String and the Method
			inputInfo.remove();
			inputInfo.remove();
		}
		
		//generate call stack
		setCallStack(formatList);
	}
	
	private void setCallStack(LinkedList<String> formatList){
		Class[] arg = new Class[1];
		for (String format : formatList){
			try {
				if (format.equals("String")){
					arg[0] = String.class;
					callStack.add(DataOutputStream.class.getMethod("writeUTF", arg));
				}
				else if (format.equals("int")){
					arg[0] = Integer.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeInt", arg));
				}
				else if (format.equals("float")){
					arg[0] = Float.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeFloat", arg));
				}
				else if (format.equals("double")){
					arg[0] = Double.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeDouble", arg));
				}
				else if (format.equals("long")){
					arg[0] = Long.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeLong", arg));
				}
				else if (format.equals("byte")){
					arg[0] = Byte.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeByte", arg));
				}
				else if (format.equals("short")){
					arg[0] = Short.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeShort", arg));
				}
				else if (format.equals("char")){
					arg[0] = Character.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeChar", arg));
				}
				else if (format.equals("boolean")){
					arg[0] = Boolean.TYPE;
					callStack.add(DataOutputStream.class.getMethod("writeBoolean", arg)) ;
				} else {
					throw new NoSuchMethodException();
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}

}
