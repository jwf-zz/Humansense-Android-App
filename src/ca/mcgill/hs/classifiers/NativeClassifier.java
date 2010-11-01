/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers;

/**
 * Wraps the native C code for the classifier.
 * 
 * @author jfrank8
 * 
 */
public class NativeClassifier {

	static {
		System.loadLibrary("humansense");
	}

	public native void annClose();

	public native float annDist(int dim, float[] p, float[] q);

	// Builds a model from data in in_file with embedding dimension m,
	// PCA reduced dimension p, and delay time d. Model is saved in a file
	// constructed from in_file with .dmp appended.
	public native void buildTree(String in_file, int m, int p, int d);

	// Classifies the data in the array in, starting from index offset, and
	// returned values are stored in output, which must be an array of length
	// getNumModels()
	public native void classifySample(float[] in, int startIndex, float[] out);

	// Loads models from models_file, then classifies data from in_file,
	// storing the class-likelihoods in out_file.
	public native void classifyTrajectory(String in_file, String out_file,
			String models_file);

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

	// Loads models from models_file.
	public native void loadModels(String models_file, int numNeighbours,
			int matchSteps);

	public native void setAlgorithmNumber(int algNum);

}
