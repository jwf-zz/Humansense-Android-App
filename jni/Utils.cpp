/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * Utils.cpp
 */

#include <stdlib.h>
#include <cstring>
#include <ctype.h>

#include <ANN/ANN.h>
#include <Tisean/tsa.h>
#include <android/log.h>
#include <cxcore/cxcore.h>
#include "Utils.h"

void get_embedding(Settings* settings, ANNcoord* &data, uint &length) {
    ulong i;
    uint j,k;
    uint alldim, maxemb, emb, rundel, delsum;
    uint *inddelay;
    uint *formatlist;
    double** series;

    check_alloc(formatlist=(uint*)malloc(sizeof(int)*settings->indim));
    for (i=0;i<settings->indim;i++) {
        formatlist[i]=settings->embdim/settings->indim;
    }
    alldim=0;
    for (i=0;i<settings->indim;i++)
        alldim += formatlist[i];
    check_alloc(inddelay=(uint*)malloc(sizeof(int)*alldim));

    rundel=0;
    for (i=0;i<settings->indim;i++) {
        delsum=0;
        inddelay[rundel++]=delsum;
        for (j=1;j<formatlist[i];j++) {
            delsum += settings->delay;
            inddelay[rundel++]=delsum;
        }
    }
    maxemb=0;
    for (i=0;i<alldim;i++)
        maxemb=(maxemb<inddelay[i])?inddelay[i]:maxemb;

    if (settings->column == NULL) {
        series=get_multi_series(settings->infile,&settings->length,settings->exclude,&settings->indim,(char*)"",settings->dimset,settings->verbosity);
    } else {
        series=get_multi_series(settings->infile,&settings->length,settings->exclude,&settings->indim,settings->column,settings->dimset,settings->verbosity);
    }
    __android_log_print(ANDROID_LOG_DEBUG, "HUMANSENSE",
    		"\tLength: %d\n\tEmbed Dim: %d\n", settings->length, settings->embdim);
    //cerr << "Length: " << settings->length << endl << "Embed Dim: " << settings->embdim << endl;

    check_alloc(data = (ANNcoord*)calloc((settings->length-maxemb)*settings->embdim,sizeof(ANNcoord)));
    uint step = settings->embdim;
    for (i=maxemb;i<settings->length;i++) {
        rundel=0;
        for (j=0;j<settings->indim;j++) {
            emb=formatlist[j];
            for (k=0;k<emb;k++)
                data[(i-maxemb)*step+(emb-k-1)] = (ANNcoord)series[j][i-inddelay[rundel++]];
        }
    }
    length = settings->length - maxemb;

    for (j=0; j < settings->indim; j++) {
    	free(series[j]);
    }
    free(series);
    free(formatlist);
    free(inddelay);

}

void get_ann_points(ANNpointArray &dataPts, ANNcoord* series, unsigned long  rows, uint cols)
{
	unsigned long k = 0;
    dataPts = annAllocPts(rows, cols);
    for (ulong i = 0; i < rows; i++) {
        for (ulong j = 0; j < cols; j++) {
        	dataPts[i][j] = series[k++];
        }
    }
}

void convert_to_ann_points(ANNpointArray &dataPts, ANNcoord* series, uint  rows, uint  cols)
{
	uint k = 0, i, j;
	// Assume Allocated
	//if (dataPts == NULL)
	//	dataPts = annAllocPts(rows, cols);
    for (i = 0; i < rows; i++) {
        for (j = 0; j < cols; j++) {
        	dataPts[i][j] = series[k++];
        }
    }
}

void print_matrix(CvMat* matrix, FILE *f) {
	int i,j;
	for (i = 0; i < matrix->rows; i++) {
		fprintf(f, FLOAT_OUT, CV_MAT_ELEM(*matrix, ANNcoord, i, 0)); // cout << CV_MAT_ELEM(*matrix, double, i, 0);
		for (j = 1; j < matrix->cols; j++) {
			fprintf(f, " " FLOAT_OUT, CV_MAT_ELEM(*matrix, ANNcoord, i, j)); // cout << " " << CV_MAT_ELEM(*matrix, double, i, j);
		}
		fprintf(f,"\n"); // cout << endl;
	}
}
