/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * ClassifyTrajectory.h
 *
 */

#include "TDEModel.h"

#ifndef CLASSIFYTRAJECTORY_H_
#define CLASSIFYTRAJECTORY_H_

typedef struct {
	TDEModel* model;
	char* name;
} NamedModel;

class ClassifyTrajectory {
public:
	ClassifyTrajectory();
	virtual ~ClassifyTrajectory();
};

void loadModels(const char* ms_file, int numNeighbours=2, int matchSteps=16);
void classifyTrajectory(const char* in_file, const char* out_file);
void cleanUpModels();

#endif /* CLASSIFYTRAJECTORY_H_ */
