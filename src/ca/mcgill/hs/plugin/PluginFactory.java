package ca.mcgill.hs.plugin;

import android.content.Context;

public final class PluginFactory {
	private static Context context = null;

	// Input Plugins
	private static BluetoothLogger bluetoothLogger = null;
	private static GPSLogger gpsLogger = null;
	private static GSMLogger gsmLogger = null;
	private static SensorLogger sensorLogger = null;
	private static WifiLogger wifiLogger = null;
	private static FileOutput fileOutput = null;

	// Output Plugins
	private static ScreenOutput screenOutput = null;
	private static SimpleClassifierPlugin simpleClassifier = null;
	private static TestMagOutputPlugin testMagGraph = null;

	private final static Class<?>[] outputPluginClasses = { FileOutput.class,
			ScreenOutput.class, TestMagOutputPlugin.class,
			SimpleClassifierPlugin.class };

	private final static Class<?>[] inputPluginClasses = {
			BluetoothLogger.class, GPSLogger.class, GSMLogger.class,
			SensorLogger.class, WifiLogger.class };

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
		InputPlugin plugin = null;
		if (type.equals(BluetoothLogger.class)) {
			if (bluetoothLogger == null) {
				bluetoothLogger = new BluetoothLogger(context);
			}
			plugin = bluetoothLogger;
		} else if (type.equals(GPSLogger.class)) {
			if (gpsLogger == null) {
				gpsLogger = new GPSLogger(context);
			}
			plugin = gpsLogger;
		} else if (type.equals(GSMLogger.class)) {
			if (gsmLogger == null) {
				gsmLogger = new GSMLogger(context);
			}
			plugin = gsmLogger;
		} else if (type.equals(WifiLogger.class)) {
			if (wifiLogger == null) {
				wifiLogger = new WifiLogger(context);
			}
			plugin = wifiLogger;
		} else if (type.equals(SensorLogger.class)) {
			if (sensorLogger == null) {
				sensorLogger = new SensorLogger(context);
			}
			plugin = sensorLogger;
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
		OutputPlugin plugin = null;
		if (type.equals(FileOutput.class)) {
			if (fileOutput == null) {
				fileOutput = new FileOutput(context);
			}
			plugin = fileOutput;
		} else if (type.equals(ScreenOutput.class)) {
			if (screenOutput == null) {
				screenOutput = new ScreenOutput(context);
			}
			plugin = screenOutput;
		} else if (type.equals(SimpleClassifierPlugin.class)) {
			if (simpleClassifier == null) {
				simpleClassifier = new SimpleClassifierPlugin(context);
			}
			plugin = simpleClassifier;
		} else if (type.equals(TestMagOutputPlugin.class)) {
			if (testMagGraph == null) {
				testMagGraph = new TestMagOutputPlugin(context);
			}
			plugin = testMagGraph;
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
