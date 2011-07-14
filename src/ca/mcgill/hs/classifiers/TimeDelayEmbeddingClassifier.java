/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers;

import java.io.File;

/**
 * Wraps the functionality of the NativeClassifier, and makes it much easier to
 * use by a plugin.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
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

	/**
	 * Adds a sample to the data buffer. Returns the index in the buffer at
	 * which the data was added.
	 * 
	 * @param sample
	 *            Data element to be added to the buffer.
	 * @return Index in the buffer where the data can be found.
	 */
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

	/**
	 * Just wraps the buildTree method of the native classifier.
	 * 
	 * @param modelFile
	 *            The file containing the data. This file must contain a single
	 *            column of floating point values. The model will be stored in a
	 *            file with the same name but with the suffix .dmp appended to
	 *            it.
	 * @param m
	 *            Initial embedding dimension.
	 * @param p
	 *            Number of principal components to use for final model.
	 * @param d
	 *            Time delay in samples.
	 * 
	 * @see NativeClassifier#buildTree
	 */
	public void buildModel(final String modelFile, final int m, final int p,
			final int d) {
		nativeClassifier.buildTree(modelFile, m, p, d);
	}

	/**
	 * Classifies the data in the buffer starting at the specified index.
	 * 
	 * @param index
	 *            The starting index in the buffer.
	 * @return The scores from each of the models. This will be an array
	 *         containing the same number of values as there are models, and the
	 *         order of the values correspond to the order of the models
	 *         returned by {@link #getLoadedModelNames()}
	 */
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

	/**
	 * Loads a set of models
	 * 
	 * @param models
	 *            A file containing the filenames of the models to load, one per
	 *            line.
	 */
	public void loadModels(final File models) {
		nativeClassifier.loadModels(models.getAbsolutePath(), 3, 8);
		nativeClassifier
				.setAlgorithmNumber(NativeClassifier.GTMALGORITHM_INDEP_STEPS);

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
