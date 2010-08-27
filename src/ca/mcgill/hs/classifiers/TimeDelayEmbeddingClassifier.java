package ca.mcgill.hs.classifiers;

import java.io.File;

public class TimeDelayEmbeddingClassifier {
	private static final String TAG = "TimeDelayEmbeddingClassifier";

	// Circular buffer for data.
	private float[] buffer;
	private int bufferIndex = 0;
	private int bufferMidPoint;
	private int bufferLength;
	private int windowLength;
	private float[] classProbs;

	private int numLoadedModels;

	static {
		System.loadLibrary("humansense");
	}

	public int addSample(final float sample) {
		int index;
		synchronized (buffer) {
			buffer[bufferIndex] = sample;
			if (bufferIndex >= bufferMidPoint) {
				if (bufferIndex > bufferMidPoint) {
					buffer[bufferIndex - windowLength] = sample;
				}
			}
			index = bufferIndex;
			bufferIndex += 1;
			if (bufferIndex >= bufferLength) {
				bufferIndex = bufferMidPoint;
			}
		}
		return index;
	}

	private native void annClose();

	private native float annDist(int dim, float[] p, float[] q);

	// Builds a model from data in in_file with embedding dimension m,
	// PCA reduced dimension p, and delay time d. Model is saved in a file
	// constructed from in_file with .dmp appended.
	private native void buildTree(String in_file, int m, int p, int d);

	public float[] classify(final int index) {
		if (index < bufferMidPoint) {
			return null;
		}
		final float[] buf = new float[windowLength];
		synchronized (buffer) {
			for (int i = 0; i < windowLength; i++) {
				buf[i] = buffer[index - windowLength + 1 + i];
			}
		}
		classifySample(buf, 0, classProbs);
		String message = "Class Probs: ";
		for (int i = 0; i < classProbs.length; i++) {
			message = message + classProbs[i] + "\t";
		}
		// Log.d(TAG, message);
		return classProbs;
	}

	// Classifies the data in the array in, starting from index offset, and
	// returned values are stored in output, which must be an array of length
	// getNumModels()
	private native void classifySample(float[] in, int startIndex, float[] out);

	// Loads models from models_file, then classifies data from in_file,
	// storing the class-likelihoods in out_file.
	public native void classifyTrajectory(String in_file, String out_file,
			String models_file);

	/**
	 * Closes the models files.
	 */
	public void close() {
		annClose();
	}

	// Cleans up any loaded models.
	public native void deleteModels();

	// Returns a tab-separated list of model names
	public native String getModelNames();

	// Returns the number of loaded models
	public native int getNumModels();

	// Returns the minimum number of samples that must be passed to the
	// classifySamples.
	// It is the maximum of the window sizes required for all of the models.
	public native int getWindowSize();

	public void loadModels(final File models) {
		loadModels(models.getAbsolutePath());
		setAlgorithmNumber(1);

		// Prepare the buffer
		windowLength = getWindowSize();
		bufferLength = windowLength * 2 - 1;
		bufferMidPoint = windowLength - 1;
		buffer = new float[bufferLength];
		bufferIndex = 0;

		numLoadedModels = getNumModels();
		classProbs = new float[numLoadedModels];
	}

	// Loads models from models_file.
	public native void loadModels(String models_file);

	private native void setAlgorithmNumber(int algNum);
}
