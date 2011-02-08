/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers;

/**
 * Wraps the native C code for the classifier.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class NativeClassifier {

	static {
		System.loadLibrary("humansense");
	}

	/**
	 * The original GTM algorithm (Frank et al., AAAI'10), which treats each
	 * step in the segment individually, that is that nearest neighbours are
	 * recomputed at each segment.
	 */
	public static final int GTMALGORITHM_INDEP_STEPS = 1;

	/**
	 * More brittle, but faster algorithm that matches the entire trace, that is
	 * the nearest neighbours are only computed for the starting point of the
	 * segment, and then the next points in the model are used as the
	 * "neighbours" for the next points in the trace, rather than being
	 * recomputed as in GTMALGORITHM_INDEP_STEPS. This algorithm should only be
	 * used if the segment length parameter is small.
	 */
	public static final int GTMALGORITHM_FULL_MATCH = 2;

	/**
	 * Experimental algorithm, needs more testing. Doesn't just find nearest
	 * neighbours in the model, but finds the closest point (approximate) on any
	 * line-segment in the model. Then this point along the line-segment is
	 * used, and a point midway on the next line segment is used as the next
	 * point. This should lead to better results, and is still fairly fast, but
	 * it's unclear that the approximations made in the implementation are
	 * sound.
	 */
	public static final int GTMALGORITHM_SEGMENT_MATCH = 3;

	/**
	 * Closes and frees the memory for the nearest-neighbour data structure.
	 */
	public native void annClose();

	/**
	 * Computes the squared distance between points p and q, each having dim
	 * dimensions.
	 * 
	 * @param dim
	 *            The dimension of the points.
	 * @param p
	 *            First point.
	 * @param q
	 *            Second point.
	 * @return Squared interpoint distance.
	 */
	public native float annDist(int dim, float[] p, float[] q);

	/**
	 * Builds a time-delay embedding model from data in in_file with embedding
	 * dimension m, PCA reduced dimension p, and delay time d. Model is saved in
	 * a file constructed from in_file with .dmp appended.
	 * 
	 * @param in_file
	 *            File name containing a single column of data points, from
	 *            which the model is built.
	 * @param m
	 *            Initial embedding dimension.
	 * @param p
	 *            Number of principal components to use for final model.
	 * @param d
	 *            Delay time in samples.
	 */
	public native void buildTree(String in_file, int m, int p, int d);

	/**
	 * Classifies the data in the array in, starting from index offset, and
	 * returned values are stored in output, which must be an array of length
	 * getNumModels().
	 * 
	 * @param in
	 *            Data to be classified.
	 * @param startIndex
	 *            Starting index from which to classify for array in.
	 * @param out
	 *            Scores, one per model. out must be preallocated with size
	 *            equal to the number of models.
	 */
	public native void classifySample(float[] in, int startIndex, float[] out);

	/**
	 * Classifies an entire trajectory in the file specified by in_file, using
	 * the models from models_file, and storing the scores in out_file.
	 * 
	 * @param in_file
	 *            A file containing a single column of values representing the
	 *            trajectory to be classified.
	 * @param out_file
	 *            The file that will contain the scores. If the file exists, it
	 *            will be overwritten.
	 * @param models_file
	 *            A file containing the file names of the model files, one per
	 *            line.
	 */
	public native void classifyTrajectory(String in_file, String out_file,
			String models_file);

	/**
	 * Deletes any models and frees the memory allocated to them.
	 */
	public native void deleteModels();

	/**
	 * Returns a tab-separated list of model names
	 * 
	 * @return A tab-separated list of model names.
	 */
	public native String getModelNames();

	/**
	 * Returns the number of loaded models
	 * 
	 * @return The number of loaded models
	 */
	public native int getNumModels();

	/**
	 * Returns the minimum number of samples that must be passed to the
	 * classifySamples. It is the maximum of the window sizes required for all
	 * of the models.
	 * 
	 * @return The minimum allowable size of an array that can be passed to
	 *         classifySamples
	 */
	public native int getWindowSize();

	/**
	 * Loads the models specified in models_file, and initializes the classifier
	 * parameters.
	 * 
	 * @param models_file
	 *            A file containing the model files, one per line.
	 * @param numNeighbours
	 *            The number of neighbours used in the nearest-neighbours step
	 *            of the classifier.
	 * @param matchSteps
	 *            The length of the sequence that is compared by the classifier
	 *            to compute the score.
	 */
	public native void loadModels(String models_file, int numNeighbours,
			int matchSteps);

	/**
	 * Select the algorithm used by the classifier.
	 * 
	 * @param algNum
	 *            Algorithm number, must be one of GTMALGORITHM_INDEP_STEPS,
	 *            GTMALGORITHM_FULL_MATCH, or GTMALGORITHM_SEGMENT_MATCH.
	 */
	public native void setAlgorithmNumber(int algNum);

}
