package ca.mcgill.hs.plugin;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

public final class PluginFactory {
	private static Context context = null;

	private final static Class<?>[] outputPluginClasses = { FileOutput.class,
			ScreenOutput.class, TestMagOutputPlugin.class,
			SimpleClassifierPlugin.class, LocationClusterer.class };

	private final static Class<?>[] inputPluginClasses = {
			BluetoothLogger.class, GPSLogger.class, GSMLogger.class,
			SensorLogger.class, WifiLogger.class };

	private static final Map<Class<? extends OutputPlugin>, OutputPlugin> outputPlugins = new HashMap<Class<? extends OutputPlugin>, OutputPlugin>();
	private static final Map<Class<? extends InputPlugin>, InputPlugin> inputPlugins = new HashMap<Class<? extends InputPlugin>, InputPlugin>();

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
			} catch (final Exception e) {
				e.printStackTrace();
			}
			inputPlugins.put(type, plugin);
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
			} catch (final Exception e) {
				e.printStackTrace();
			}
			outputPlugins.put(type, plugin);
		}
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends OutputPlugin>[] getOutputPluginClassList() {
		return (Class<? extends OutputPlugin>[]) outputPluginClasses;
	}

	public static void setContext(final Context context) {
		PluginFactory.context = context;
	}
}
