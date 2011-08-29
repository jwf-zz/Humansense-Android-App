/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * TDEModel.h
 */

#include <cxcore/cxcore.h>
#include "Utils.h"

#ifndef TDEMODEL_H_
#define TDEMODEL_H_

class TDEModel {
public:
	TDEModel(Settings* settings);
	TDEModel(FILE *model_file);
	virtual ~TDEModel();

	void DumpTree(char* outfile);
	void getKNN(ANNpoint ap, uint k, ANNidxArray nn_idx, ANNdistArray dists);
	void simulateTrajectory(ANNpoint s0, ANNpointArray trajectory, uint dim, ulong  N);
    ANNpoint getDataPoint(uint idx);
    ANNcoord *projectData(ANNcoord *data, uint rows, uint cols);

    uint getLength() const { return length; }
    uint getEmbDim() const { return embdim; }
    uint getDelay() const { return delay; }
    bool getUsePCA() const { return use_pca; }
    uint getPCAEmbDim() const {
    	if (use_pca) {
    		return bases->cols;
    	}
    	else {
    		return embdim;
    	}
    }
private:
    uint length, embdim, delay;
    ANNpointArray dataPts;
    ANNkd_tree *kdTree;
    // Related to the PCA
    void computePCABases(ANNcoord *data, uint rows, uint cols, uint numbases);
    bool use_pca;
    CvMat* avg;
    CvMat* bases;
};

#endif /* TDEMODEL_H_ */
