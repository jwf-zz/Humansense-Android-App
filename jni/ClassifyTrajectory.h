/*
 * ClassifyTrajectory.h
 *
 *  Created on: 2009-06-30
 *      Author: jfrank8
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

void loadModels(const char* ms_file);
void classifyTrajectory(const char* in_file, const char* out_file);
void cleanUpModels();

#endif /* CLASSIFYTRAJECTORY_H_ */
