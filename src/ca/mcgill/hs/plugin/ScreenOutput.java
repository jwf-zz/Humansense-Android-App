package ca.mcgill.hs.plugin;

import android.util.Log;


/**
 * This output plugin takes data from a ReadableByteChannel and outputs it to the
 * Android's logcat.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class ScreenOutput extends OutputPlugin{

	/**
	 * Whenever a thread from this plugin recieves data, it simply writes out the data to the
	 * Android's logcat in the order recieved. The log's tag is the name of the source input
	 * plugin.
	 */
	void onDataReady(Object[] data, int sourceId) {
		for (int i = 0; i < data.length; i++){
			Log.i(getSourceName(sourceId), data[i].toString());
		}
	}	

}
