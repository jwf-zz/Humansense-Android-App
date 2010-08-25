/*
 * Utils.h
 *
 *  Created on: 2009-06-30
 *      Author: jfrank8
 */

#include <stdlib.h>
#include <ANN/ANN.h>
#include <cxcore/cxcore.h>
#include <android/log.h>

#ifndef ulong
#define ulong unsigned long
#endif
#ifndef uint
#define uint unsigned int
#endif

#ifndef UTILS_H_
#define UTILS_H_

typedef struct {
	ulong length;
	ulong exclude;
	uint verbosity;
	int delay;
	uint indim;
	uint embdim;
	uint pcaembdim;
	char *column;
	char *infile;
	char *outfile;
	char dimset;
	char embset;
	char pcaembset;
	char delayset;
	char stdo;
} Settings;

void get_embedding(Settings* settings, ANNcoord*& data, uint &length);
void convert_to_ann_points(ANNpointArray &dataPts, ANNcoord* series, uint rows, uint cols);
void get_ann_points(ANNpointArray &dataPts, ANNcoord* series, unsigned long rows, uint cols);
void print_matrix(CvMat* matrix, FILE *fout);

#define HS_TAG "HUMANSENSE"
#define MAT_TYPE CV_32FC1
#define FLOAT_SCAN "%G"
#define FLOAT_OUT "%.8G"

#define LOG(s) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s)
#define LOG2(s,...) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, __VA_ARGS__)
//#define LOG1(s,p1) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, p1)
//#define LOG2(s,p1,p2) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, p1, p2)

#endif /* UTILS_H_ */
