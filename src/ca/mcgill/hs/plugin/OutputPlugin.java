package ca.mcgill.hs.plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * A simple interface detailing the behaviour of data printing plugins.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public abstract class OutputPlugin implements Plugin {
	
	protected final LinkedList<DataInputStream> disList = new LinkedList<DataInputStream>();
	protected final LinkedList<LinkedList<String>> formatListList = new LinkedList<LinkedList<String>>();
	protected final LinkedList<LinkedList<String>> nameListList = new LinkedList<LinkedList<String>>();
	protected final LinkedList<LinkedList<Method>> methodListList = new LinkedList<LinkedList<Method>>();
	
	public void connect(DataInputStream dis, LinkedList<Object> connectionInfo){
		disList.add(dis);
	/*	
		LinkedList<String> formatList = new LinkedList<String>();
		LinkedList<String> nameList = new LinkedList<String>();
		LinkedList<Method> methodList = new LinkedList<Method>();
		
		while (!connectionInfo.isEmpty()){
			formatList.add((String) connectionInfo.remove());
			nameList.add((String) connectionInfo.remove());
			methodList.add((Method) connectionInfo.remove());
		}
		
		formatListList.add(formatList);
		nameListList.add(nameList);
		methodListList.add(methodList);*/
	}

}
