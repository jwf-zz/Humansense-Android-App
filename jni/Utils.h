/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * Utils.h
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
	char stdo;
} Settings;

void get_embedding(Settings* settings, ANNcoord*& data, uint &length);
void convert_to_ann_points(ANNpointArray &dataPts, ANNcoord* series, uint rows, uint cols);
void get_ann_points(ANNpointArray &dataPts, ANNcoord* series, unsigned long rows, uint cols);
void print_matrix(CvMat* matrix, FILE *fout);
void check_alloc(void *pointer);
double **get_multi_series(char *name, unsigned long *l, unsigned long ex,
		unsigned int *col, char *which, char colfix, unsigned int verbosity);
char* getline(char *str, int *size, FILE *fin, unsigned int verbosity);

#define HS_TAG "HUMANSENSE"
#define MAT_TYPE CV_32FC1
#define FLOAT_SCAN "%G"
#define FLOAT_OUT "%.8G"
#define CHECK_ALLOC_NOT_ENOUGH_MEMORY 12
#define GET_MULTI_SERIES_WRONG_TYPE_OF_C 21
#define GET_MULTI_SERIES_NO_LINES 22

// Defines the buffer size for reading lines.
#define INPUT_SIZE 1024

/* The possible names of the verbosity levels */
#define VER_INPUT 0x1
#define VER_USR1 0x2
#define VER_USR2 0x4
#define VER_USR3 0x8
#define VER_USR4 0x10
#define VER_USR5 0x20
#define VER_USR6 0x40
#define VER_FIRST_LINE 0x80

#define HS_LOG(s) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s)
#define HS_LOG2(s,...) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, __VA_ARGS__)
//#define LOG1(s,p1) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, p1)
//#define LOG2(s,p1,p2) __android_log_print(ANDROID_LOG_DEBUG, HS_TAG, s, p1, p2)

#endif /* UTILS_H_ */
