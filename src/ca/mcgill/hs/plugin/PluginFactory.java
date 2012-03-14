/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.plugin;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.XmlResourceParser;
import ca.mcgill.hs.util.Log;

/**
 * We use a factory pattern since we need to ensure that plugins are singletons.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public final class PluginFactory {
	private static Context context = null;
	private static final String TAG = "PluginFactory";

	private static final Map<Class<? extends OutputPlugin>, OutputPlugin> outputPlugins = new HashMap<Class<? extends OutputPlugin>, OutputPlugin>();
	private final static Class<?>[] outputPluginClasses = { FileOutput.class,
			ScreenOutput.class, TestMagOutputPlugin.class,
			TDEClassifierPlugin.class, LocationClusterer.class };

	private static final Map<Class<? extends InputPlugin>, InputPlugin> inputPlugins = new HashMap<Class<? extends InputPlugin>, InputPlugin>();
	private final static Class<?>[] inputPluginClasses = {
			BluetoothLogger.class, GPSLogger.class, GSMLogger.class,
			SensorLogger.class, WifiLogger.class, LocationLogger.class };

	public static Context getContext() {
		return context;
	}

	/**
	 * Creates a new Input Plugin or returns the already-created plugin. This
	 * ensures that plugins are singletons. Context must be set before calling
	 * this method.
	 * 
	 * @param type
	 *            Type of input plugin to be created
	 * @return A plugin of the specified type
	 */
	public static InputPlugin getInputPlugin(
			final Class<? extends InputPlugin> type) {
		InputPlugin plugin = inputPlugins.get(type);
		if (plugin == null) {
			try {
				plugin = type.getConstructor(Context.class)
						.newInstance(context);
				inputPlugins.put(type, plugin);
			} catch (final Exception e) {
				Log.e(TAG, e);
			}
		}
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends InputPlugin>[] getInputPluginClassList() {
		return (Class<? extends InputPlugin>[]) inputPluginClasses;
	}

	/**
	 * Creates a new Output Plugin or returns the already-created plugin. This
	 * ensures that plugins are singletons. Context must be set before calling
	 * this method.
	 * 
	 * @param type
	 *            Type of output plugin to be created
	 * @return A plugin of the specified type
	 */
	public static OutputPlugin getOutputPlugin(
			final Class<? extends OutputPlugin> type) {
		OutputPlugin plugin = outputPlugins.get(type);
		if (plugin == null) {
			try {
				plugin = type.getConstructor(Context.class)
						.newInstance(context);
				outputPlugins.put(type, plugin);
			} catch (final Exception e) {
				Log.e(TAG, e);
			}
		}
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends OutputPlugin>[] getOutputPluginClassList() {
		return (Class<? extends OutputPlugin>[]) outputPluginClasses;
	}

	/**
	 * Helper function, allowing plugins to dynamically instantiate preferences
	 * that are stored as XML chunks in the application resources. For example
	 * of its use, see {@link TDEClassifierPlugin#getThresholdAttributes()}
	 * 
	 * @param id
	 *            The resource id for the XML block.
	 * @return The XML Resources associated with the id.
	 */
	public static XmlResourceParser getXmlResourceParser(final int id) {
		return context.getResources().getXml(id);
	}

	/**
	 * Sets the application context. Must be called before any of the other
	 * methods will work properly.
	 * 
	 * @param context
	 *            The application context in which the plugins will be
	 *            instantiated.
	 */
	public static void setContext(final Context context) {
		PluginFactory.context = context;
	}
}
