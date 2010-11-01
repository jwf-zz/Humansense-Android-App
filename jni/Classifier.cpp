/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * Classifier.cpp
 *
 */

#include <stdlib.h>
#include <ANN/ANN.h>
#include <android/log.h>
#include "ClassifyTrajectory.h"
#include "Classifier.h"

Classifier::Classifier(std::vector<NamedModel*> *models, uint numNeighbours, uint matchSteps) {
	int pcaembdim, i, W;
	TDEModel *model;
	windowSize = 0;
	algorithm = 1; // Default to first algorithm
	this->numNeighbours = numNeighbours;
	this->matchSteps = matchSteps;
	if (models == NULL) {
		this->models = NULL;
		this->numModels = 0;
	} else {
		this->models = models;
		numModels = models->size();

		navg = new CvMat*[numModels];
		navg_next = new CvMat*[numModels];
		proj_next = new CvMat*[numModels];
		nn = new CvMat*[numModels];
		nnn = new CvMat*[numModels];
		for (i = 0; i < numModels; i++) {
			model = (*models)[i]->model;
			W = (model->getEmbDim() - 1) * model->getDelay() + matchSteps + 1;
			if (W > windowSize)
				windowSize = W;
			pcaembdim = model->getPCAEmbDim();
			navg[i] = cvCreateMat(1, pcaembdim, MAT_TYPE);
			navg_next[i] = cvCreateMat(1, pcaembdim, MAT_TYPE);
			proj_next[i] = cvCreateMat(1, pcaembdim, MAT_TYPE);
			nn[i] = cvCreateMat(numNeighbours, pcaembdim, MAT_TYPE);
			nnn[i] = cvCreateMat(numNeighbours, pcaembdim, MAT_TYPE);
		}
	}
}

Classifier::~Classifier() {
	HS_LOG("Classifier Destructor Called.");

	if (models != NULL) {
		for (uint i = 0; i < numModels; i++) {
			cvReleaseMat(&navg[i]);
			cvReleaseMat(&navg_next[i]);
			cvReleaseMat(&nn[i]);
			cvReleaseMat(&nnn[i]);
			free((*models)[i]->name);
			delete (*models)[i]->model;
			free((*models)[i]);
		}
		delete[] navg;
		delete[] navg_next;
		delete[] proj_next;
		delete[] nn;
		delete[] nnn;

		models->clear();
		delete models;
	}
}

uint Classifier::getNumModels() {
	return numModels;
}

uint Classifier::getNumNeighbours() {
	return numNeighbours;
}

uint Classifier::getMatchSteps() {
	return matchSteps;
}

uint Classifier::getWindowSize() {
	return windowSize;
}

char* Classifier::getModelNames() {
	if (models == NULL) {
		return NULL;
	}
	int M = this->numModels;
	int N = 0;
	for (int i = 0; i < M; i++) {
		N = N + strlen((*models)[i]->name);
	}
	char* model_names = (char*)malloc((N+M)*sizeof(char));
	model_names[0] = '\0';
	for (int i = 0; i < M; i++) {
		if (i > 0) strcat(model_names,"\t");
		strcat(model_names,(*models)[i]->name);
	}
	return model_names;
}

void Classifier::classifyAndSave(ANNcoord** data, ulong length, FILE *fout) {
	if (models == NULL) {
		return;
	}
	CvMat *mdists = classify(data, length);
	print_matrix(mdists, fout);
	cvReleaseMat(&mdists);
}

CvMat* Classifier::classify(ANNcoord** data, ulong length) {
	if (models == NULL) {
		return NULL;
	}

	TDEModel* model;
	int M = this->numModels;
	uint extra_neighbours = 0;
	if (algorithm == 2) {
		extra_neighbours = 32;
	}
	else if (algorithm == 3) {
		extra_neighbours = 5;
	}

	char* model_name;
	ANNcoord *projected_data;
	ANNpointArray ap;
	ANNidx nn_idx[numNeighbours + extra_neighbours + 1];
	ANNdist dists[numNeighbours + extra_neighbours + 1];
	ANNdist mdist;
	CvMat p, np, *mdists;
	uint i, j, k, l, a, pcaembdim;
	uint N;
	ANNdist dist, dist_next, *dst, l1, l2;
	ANNcoord *p1, *p2, *p3, *p4, *p5, interpcoeff;
	mdists = cvCreateMat(length - matchSteps + 1, M, MAT_TYPE);
	cvZero(mdists);

	for (i = 0; i < length - matchSteps + 1; i++) {
		for (k = 0; k < M; k++) {
			model = (*models)[k]->model;
			model_name = (*models)[k]->name;
			N = model->getLength();
			pcaembdim = model->getPCAEmbDim();
			mdist = 0.0;
			ap = annAllocPts(matchSteps+1,pcaembdim);
			convert_to_ann_points(ap, data[k] + i * pcaembdim, matchSteps + 1, pcaembdim);
			if (algorithm == 1) {
				// Get the next MATCH_STEP+1 data points, and convert them to the appropriate
				// format, stored in ap.
				for (j = 0; j < matchSteps; j++) {
					model->getKNN(ap[j], numNeighbours + 1, nn_idx, dists);
					for (l = 0; l < numNeighbours; l++) {

						// Make sure none of the first numNeighbours neighbours is N
						if (nn_idx[l] == ANN_NULL_IDX)
							break;
						else if ((uint) nn_idx[l] == N - 1)
							nn_idx[l] = nn_idx[numNeighbours];

						// p1 and p2 are just pointers to the data in nn and nnn, respectively.
						p1 = (ANNcoord*) (nn[k]->data.ptr + l * nn[k]->step);
						p2 = (ANNcoord*) (nnn[k]->data.ptr + l * nnn[k]->step);
						// p3 and p4 point to the l'th nearest neighbor of our point and its successor, respectively.
						p3 = model->getDataPoint(nn_idx[l]);
						p4 = model->getDataPoint(nn_idx[l] + 1);
						// This just copies the data from the model into our nn and nnn variables.
						// This may be a bit slow, but it makes things easier. Maybe we could use
						// memcpy to speed this up a bit.
						for (a = 0; a < pcaembdim; a++) {
							*p1++ = *p3++;
							*p2++ = *p4++;
						}
					}
					if (l < numNeighbours) {
						HS_LOG2("Couldn't find enough neighbours (found: %d, required: %d).", l, numNeighbours);
					}

					// Computes the mean of the nearest neighbours.
					cvReduce(nn[k], navg[k], 0, CV_REDUCE_AVG);

					// Computes the mean of the neigbours' successors
					cvReduce(nnn[k], navg_next[k], 0, CV_REDUCE_AVG);

					// The projected next point is computed by taking our data point and adding to
					// it the vector from navg and navg_next.
					p1 = (ANNcoord*) navg_next[k]->data.ptr;
					p2 = (ANNcoord*) navg[k]->data.ptr;
					dst = (ANNcoord*) proj_next[k]->data.ptr;
					for (l = 0; l < pcaembdim; l++) {
						*dst++ = ap[j][l] + (*p1++ - *p2++);
					}

					// p is the current point in the trajectory that we want to classify.
					p = cvMat(1, pcaembdim, MAT_TYPE, ap[j]);// &projected_data[j*pcaembdim]);

					// np is the subsequent point in the trajectory to be classified.
					np = cvMat(1, pcaembdim, MAT_TYPE, ap[j + 1]);

					// Shift each vector to the origin and compute the dot product
					p1 = (ANNcoord*)p.data.ptr;
					p2 = (ANNcoord*)np.data.ptr;
					p3 = (ANNcoord*)proj_next[k]->data.ptr;
					dist = 0;
					l1 = 0.0; // Length of first vector
					l2 = 0.0; // Length of second vector
					for (l = 0; l < pcaembdim; l++) {
							dist = dist + (*p2 - *p1)*(*p3 - *p1);
							l1 = l1 + (*p2 - *p1)*(*p2 - *p1);
							l2 = l2 + (*p3 - *p1)*(*p3 - *p1);
							*p1++; *p2++; *p3++;
					}
					if (MAX(l1,l2) > 0.0f) {
						mdist = mdist + dist/MAX(l1,l2);
					}
				}
			}
			else if (algorithm == 2) {
				// Just get the nearest neighbour of the first point.
				model->getKNN(ap[0], numNeighbours + extra_neighbours + 1, nn_idx, dists);
				for (j = 0; j < matchSteps; j++) {
					for (l = 0; l < numNeighbours; l++) {
						// Make sure none of the first numNeighbours neighbours is N
						if (nn_idx[l] == ANN_NULL_IDX) {
							break;
						}
						a = 0;
						while ((uint)nn_idx[l] > N-matchSteps-1) {
//							__android_log_print(
//									ANDROID_LOG_DEBUG,
//									HS_TAG,
//									"Bad neighbour %d.", nn_idx[l]);
							nn_idx[l] = nn_idx[numNeighbours+a++];
							if (a >= extra_neighbours) {
								HS_LOG("Couldn't find enough good neighbours.");
								nn_idx[l] = 0;
								break;
							}
						}

						// p1 and p2 are just pointers to the data in nn and nnn, respectively.
						p1 = (ANNcoord*) (nn[k]->data.ptr + l * nn[k]->step);
						p2 = (ANNcoord*) (nnn[k]->data.ptr + l * nnn[k]->step);
						// p3 and p4 point to the l'th nearest neighbor of our point and its successor, respectively.
						p3 = model->getDataPoint(nn_idx[l]+j);
						p4 = model->getDataPoint(nn_idx[l]+j+1);
						// This just copies the data from the model into our nn and nnn variables.
						// This may be a bit slow, but it makes things easier. Maybe we could use
						// memcpy to speed this up a bit.
						for (a = 0; a < pcaembdim; a++) {
							*p1++ = *p3++;
							*p2++ = *p4++;
						}
					}
					if (l < numNeighbours) {
						HS_LOG2("Couldn't find enough neighbours (found: %d, required: %d).", l, numNeighbours);
					}

					// Computes the mean of the nearest neighbours.
					cvReduce(nn[k], navg[k], 0, CV_REDUCE_AVG);

					// Computes the mean of the neigbours' successors
					cvReduce(nnn[k], navg_next[k], 0, CV_REDUCE_AVG);

					// The projected next point is computed by taking our data point and adding to
					// it the vector from navg and navg_next.
					p1 = (ANNcoord*) navg_next[k]->data.ptr;
					p2 = (ANNcoord*) navg[k]->data.ptr;
					dst = (ANNcoord*) proj_next[k]->data.ptr;

					for (l = 0; l < pcaembdim; l++) {
						*dst++ = ap[j][l] + (*p1++ - *p2++);
					}


					// p is the current point in the trajectory that we want to classify.
					p = cvMat(1, pcaembdim, MAT_TYPE, ap[j]);// &projected_data[j*pcaembdim]);
					// np is the subsequent point in the trajectory to be classified.
					np = cvMat(1, pcaembdim, MAT_TYPE, ap[j + 1]);

					// Shift each vector to the origin and compute the dot product
					p1 = (ANNcoord*)p.data.ptr;
					p2 = (ANNcoord*)np.data.ptr;
					p3 = (ANNcoord*)proj_next[k]->data.ptr;
					dist = 0;
					l1 = 0.0; // Length of first vector
					l2 = 0.0; // Length of second vector
					for (l = 0; l < pcaembdim; l++) {
							dist = dist + (*p2 - *p1)*(*p3 - *p1);
							l1 = l1 + (*p2 - *p1)*(*p2 - *p1);
							l2 = l2 + (*p3 - *p1)*(*p3 - *p1);
							*p1++; *p2++; *p3++;
					}
					if (MAX(l1,l2) > 0.0f) {
						mdist = mdist + dist/MAX(l1,l2);
					}
				}
			}
			else if (algorithm == 3) {
				// Try to reduce score variance by taking account distance from
				// line segments when constructing expected next points.
				for (j = 0; j < matchSteps; j++) {
					model->getKNN(ap[j], numNeighbours+extra_neighbours+1, nn_idx, dists);

					for (l = 0; l < numNeighbours; l++) {
						// Make sure none of the first neighbours is N, 0, or invalid.
						if (nn_idx[l] == ANN_NULL_IDX) break;
						a = 0;
						while ((uint)nn_idx[l] >= N-3 || (uint)nn_idx[l] == 0) {
							nn_idx[l] = nn_idx[numNeighbours+a++];
							if (a >= extra_neighbours) {
								HS_LOG("Couldn't find enough good neighbours.");
								nn_idx[l] = 0;
								break;
							}
						}

						// Copy in the data.
						p1 = (ANNcoord*)(nn[k]->data.ptr+l*nn[k]->step);
						p2 = (ANNcoord*)(nnn[k]->data.ptr+l*nnn[k]->step);
						p3 = model->getDataPoint(nn_idx[l]);
						p4 = model->getDataPoint(nn_idx[l]+1);
						p5 = model->getDataPoint(nn_idx[l]+2);

						interpcoeff = get_interpolation_coefficient(ap[j], p3, p4, pcaembdim);
						if (interpcoeff < 0.0f) {
							// Back up one step.
							p5 = p4;
							p4 = p3;
							p3 = model->getDataPoint(nn_idx[l]-1);
							interpcoeff = get_interpolation_coefficient(ap[j], p3, p4, pcaembdim);
						}
						else if (interpcoeff > 1.0f) {
							// Move ahead one step
							p3 = p4;
							p4 = p5;
							p5 = model->getDataPoint(nn_idx[l]+3);
							interpcoeff = get_interpolation_coefficient(ap[j], p3, p4, pcaembdim);
						}
						if (interpcoeff < 0.0f) interpcoeff = 0.0f;
						if (interpcoeff > 1.0f) interpcoeff = 1.0f;
						for (a = 0; a < pcaembdim; a++) {
							*p1++ = (1.0f - interpcoeff) * *p3 + interpcoeff * *p4;
							*p2++ = (1.0f - interpcoeff) * *p4 + interpcoeff * *p5;
							p3++; p4++; p5++;
						}
					}
					if (l < numNeighbours)
						HS_LOG("Couldn't find enough good neighbours.");

					// Computes the mean of the nearest neighbours.
					cvReduce(nn[k], navg[k], 0, CV_REDUCE_AVG );

					// Computes the mean of the neigbours' successors
					cvReduce(nnn[k], navg_next[k], 0, CV_REDUCE_AVG );

					p1 = (ANNcoord*)navg_next[k]->data.ptr;
					p2 = (ANNcoord*)navg[k]->data.ptr;
					dst = (ANNcoord*)proj_next[k]->data.ptr;
					for (l = 0; l < pcaembdim; l++) {
						*dst++ = ap[j][l] + (*p1++ - *p2++);
					}

					// p is the current point in the trajectory that we want to classify.
					p = cvMat(1, pcaembdim, MAT_TYPE, ap[j]);
					// np is the subsequent point in the trajectory to be classified.
					np = cvMat(1, pcaembdim, MAT_TYPE, ap[j + 1]);

					// Shift each vector to the origin and compute the dot product
					// normalized by the length of the larger vector.
					p1 = (ANNcoord*)p.data.ptr;
					p2 = (ANNcoord*)np.data.ptr;
					p3 = (ANNcoord*)proj_next[k]->data.ptr;
					dist = 0.0;
					l1 = 0.0; // Length of first vector
					l2 = 0.0; // Length of second vector
					for (l = 0; l < pcaembdim; l++) {
						dist = dist + (*p2 - *p1)*(*p3 - *p1);
						l1 = l1 + (*p2 - *p1)*(*p2 - *p1);
						l2 = l2 + (*p3 - *p1)*(*p3 - *p1);
						*p1++; *p2++; *p3++;
					}
					if (MAX(l1,l2) > 0.0f) {
						mdist = mdist + dist/MAX(l1,l2);
					}
				}
			}
			cvmSet(mdists, i, k, mdist);
			annDeallocPts(ap);
		}
	}
	return mdists;
}

void Classifier::setAlgorithmNumber(int alg) {
	algorithm = alg;
}

ANNcoord* Classifier::getProjectedData(int modelId, ANNcoord* input, int length) {
	int embDim, pcaEmbDim, delay;
	int i, j, k;
	ANNcoord *data, *projected;
	TDEModel *model = (*models)[modelId]->model;
	embDim = model->getEmbDim();
	pcaEmbDim = model->getPCAEmbDim();
	delay = model->getDelay();

	data = new ANNcoord[length * embDim];
	k = 0;
	for (i = 0; i < length; i++) {
		for (j = 0; j < embDim; j++) {
			data[k] = input[i + j * delay];
			k++;
		}
	}
	if (embDim != pcaEmbDim) {
		projected = model->projectData(data, length, embDim);
		delete[] data;
	} else {
		projected = data;
	}
	return projected;
}

inline float get_interpolation_coefficient(ANNpoint p, ANNpoint p1, ANNpoint p2, uint dim) {
	uint i;
	float num = 0.0, denom = 0.0;
	ANNcoord *a1 = p1, *a2 = p2, *q = p;
	for (i = 0; i < dim; i++) {
		num = num + (*q - *a1) * (*a2 - *a1);
		denom = denom + (*a2 - *a1) * (*a2 - *a1);
		a1++; a2++; q++;
	}
	return num / denom;
}
