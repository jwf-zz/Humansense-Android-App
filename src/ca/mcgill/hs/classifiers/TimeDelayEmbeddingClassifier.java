/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers;

import java.io.File;

public class TimeDelayEmbeddingClassifier {
	@SuppressWarnings("unused")
	private static final String TAG = "TimeDelayEmbeddingClassifier";

	private static final NativeClassifier nativeClassifier = new NativeClassifier();

	// Circular buffer for data.
	private float[] buffer;
	private int bufferIndex = 0;
	private int bufferMidPoint;
	private int bufferLength;
	private int windowLength;
	private float[] classProbs;

	private int numLoadedModels;

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

	public void buildModel(final String modelFile, final int m, final int p,
			final int d) {
		nativeClassifier.buildTree(modelFile, m, p, d);
	}

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

		nativeClassifier.classifySample(buf, 0, classProbs);
		// String message = "Class Probs: ";
		// for (int i = 0; i < classProbs.length; i++) {
		// message = message + classProbs[i] + "\t";
		// }
		// Log.d(TAG, message);
		return classProbs;
	}

	/**
	 * Closes the models files.
	 */
	public void close() {
		nativeClassifier.annClose();
	}

	/**
	 * Gets the loaded model names as a tab-separated list.
	 * 
	 * @return A tab-separated list of model names.
	 */
	public String getLoadedModelNames() {
		return nativeClassifier.getModelNames();
	}

	public int getNumModels() {
		return nativeClassifier.getNumModels();
	}

	public void loadModels(final File models) {
		nativeClassifier.loadModels(models.getAbsolutePath(), 2, 16);
		nativeClassifier.setAlgorithmNumber(1);

		// Prepare the buffer
		windowLength = nativeClassifier.getWindowSize();
		bufferLength = windowLength * 2 - 1;
		bufferMidPoint = windowLength - 1;
		buffer = new float[bufferLength];
		bufferIndex = 0;

		numLoadedModels = nativeClassifier.getNumModels();
		classProbs = new float[numLoadedModels];
	}
}
