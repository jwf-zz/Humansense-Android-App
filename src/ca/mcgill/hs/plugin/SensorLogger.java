package ca.mcgill.hs.plugin;

import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * An InputPlugin which gets data from the phone's available sensors.
 * 
 * @author Cicerone Cojocaru, Jonathan Pitre
 *
 */
public class SensorLogger extends InputPlugin implements SensorEventListener{
	
	//The SensorManager used to register listeners.
	private final SensorManager sensorManager;
	
	//A boolean checking whether or not we are logging at a given moment.
	private static boolean logging = false;
	
	//Variables used to write out the sensor data received.
	private static float temperature = 0.0f;
	private static float[] magfield = { 0.0f, 0.0f, 0.0f };
	private static boolean magfieldUpdated = false;
	private static float[] orientation = { 0.0f, 0.0f, 0.0f };
	
	/**
	 * This is the basic constructor for the SensorLogger plugin. It has to be instantiated
	 * before it is started, and needs to be passed a reference to a SensorManager.
	 * 
	 * @param gpsm
	 * @param context
	 */
	public SensorLogger(SensorManager sensorManager){
		this.sensorManager = sensorManager;
	}
	
	/**
	 * Registers the appropriate listeners using the SensorManager.
	 * 
	 * @override
	 */
	public void startPlugin() {
		Log.i("SensorLogger", "Registered Sensor Listener");
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE),
				SensorManager.SENSOR_DELAY_UI);
		logging = true;
	}
	
	/**
	 * This method gets called automatically whenever a sensor has changed.
	 * 
	 * @param event the SensorEvent detailing the change in sensor data.
	 * 
	 * @override
	 */
	public void onSensorChanged(SensorEvent event) {
		if (logging) {
			final Sensor sensor = event.sensor;
	        final int type = sensor.getType();	
		        switch (type) {
		        	case Sensor.TYPE_MAGNETIC_FIELD:
		        		magfield = event.values.clone();
		        		magfieldUpdated = true;
		        		break;
		        	case Sensor.TYPE_TEMPERATURE:
		        		temperature = event.values[0];
		        		break;
		        	case Sensor.TYPE_ACCELEROMETER:
						if (magfieldUpdated) {
							magfieldUpdated = false;
							final int matrix_size = 16;
							float[] R = new float[matrix_size];
							float[] I = new float[matrix_size];
							float[] outR = new float[matrix_size];
							
							SensorManager.getRotationMatrix(R, I, event.values, magfield);

			                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
			                SensorManager.getOrientation(outR, orientation);							// Update the orientation information.
						}
						logAccelerometerData(event.values, event.timestamp/1000000);
		        	}
				
			}
		}
	
	/**
	 * Processes the results sent by the Sensor change and writes them out.
	 */
	private void logAccelerometerData(final float[] values, final long timestamp) {
		final float x = values[0];
		final float y = values[1];
		final float z = values[2];
		final float m = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.STANDARD_GRAVITY;
		
		write(new SensorLoggerPacket(timestamp, x, y, z, m, temperature, magfield, orientation));
	}


	/**
	 * Unregisters the appropriate listeners using the SensorManager.
	 * 
	 * @override
	 */
	public void stopPlugin() {
		Log.i("SensorLogger", "Unregistered Sensor Listener.");
		sensorManager.unregisterListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		sensorManager.unregisterListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		sensorManager.unregisterListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE));
		logging = false;
	}


	/**
	 * This method gets called automatically whenever a Sensor's accuracy has changed.
	 * 
	 * @param sensor the Sensor whose accuracy changed.
	 * @param accuracy the new accuracy of the given Sensor.
	 * 
	 * @override
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}
	
	// ***********************************************************************************
	// PUBLIC INNER CLASS -- SensorLoggerPacket
	// ***********************************************************************************
	
	public class SensorLoggerPacket implements DataPacket{
		
		final long time;
		final float x;
		final float y;
		final float z;
		final float m;
		final float temperature;
		final float[] magfield;
		final float[] orientation;
		
		public SensorLoggerPacket(final long time, final float x, final float y, final float z, final float m, final float temperature,
				final float[] magfield, final float[] orientation){
			this.time = time;
			this.x = x;
			this.y = y;
			this.z = z;
			this.m = m;
			this.temperature = temperature;
			this.magfield = magfield;
			this.orientation = orientation;
		}
		
		@Override
		public String getInputPluginName() {
			return "SensorLogger";
		}
		
		public DataPacket clone(){
			return new SensorLoggerPacket(time, x, y, z, m, temperature, magfield, orientation);
		}

	}

}
