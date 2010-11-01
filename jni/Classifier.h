/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * Classifier.h
 *
 */

#include <stdlib.h>
#include "ClassifyTrajectory.h"
#include <vector>
#include <ANN/ANN.h>

#ifndef CLASSIFIER_H_
#define CLASSIFIER_H_

class Classifier {

private:
	uint numModels, windowSize;
	uint algorithm;
	uint numNeighbours, matchSteps;
	const static int DEFAULT_NEIGHBOURS = 2;
	const static int DEFAULT_MATCH_STEPS = 16;
public:

	Classifier(std::vector<NamedModel*> *models, uint numNeighbours=DEFAULT_NEIGHBOURS, uint matchSteps=DEFAULT_MATCH_STEPS);
	virtual ~Classifier();

	void classifyAndSave(ANNcoord** data, ulong length, FILE *fout);
	void go(ANNcoord** data, ulong length, FILE *fout);
	CvMat* classify(ANNcoord** data, ulong length);

	// Computes a time delay embedding for the specified model.
	// length should be the number of "rows" that are expected,
	// not the length of the input.
	ANNcoord* getProjectedData(int model, ANNcoord* input, int length);

	uint getNumModels();
	uint getWindowSize();
	uint getNumNeighbours();
	uint getMatchSteps();

	void setAlgorithmNumber(int alg);
	char* getModelNames();

	std::vector<NamedModel*> *models;

	CvMat **navg, **navg_next, **proj_next, **nn, **nnn;

};
inline float get_interpolation_coefficient(ANNpoint p, ANNpoint p1, ANNpoint p2, uint dim);

#endif /* CLASSIFIER_H_ */
