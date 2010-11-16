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
#include <android/log.h>
#include <cxcore/cxcore.h>
#include "Utils.h"

void get_embedding(Settings* settings, ANNcoord* &data, uint &length) {
	ulong i;
	uint j, k;
	uint alldim, maxemb, emb, rundel, delsum;
	uint *inddelay;
	uint *formatlist;
	double** series;

	check_alloc(formatlist = (uint*) malloc(sizeof(int) * settings->indim));
	for (i = 0; i < settings->indim; i++) {
		formatlist[i] = settings->embdim / settings->indim;
	}
	alldim = 0;
	for (i = 0; i < settings->indim; i++)
		alldim += formatlist[i];
	check_alloc(inddelay = (uint*) malloc(sizeof(int) * alldim));

	rundel = 0;
	for (i = 0; i < settings->indim; i++) {
		delsum = 0;
		inddelay[rundel++] = delsum;
		for (j = 1; j < formatlist[i]; j++) {
			delsum += settings->delay;
			inddelay[rundel++] = delsum;
		}
	}
	maxemb = 0;
	for (i = 0; i < alldim; i++)
		maxemb = (maxemb < inddelay[i]) ? inddelay[i] : maxemb;

	if (settings->column == NULL) {
		series = get_multi_series(settings->infile, &settings->length,
				settings->exclude, &settings->indim, (char*) "",
				settings->dimset, settings->verbosity);
	} else {
		series = get_multi_series(settings->infile, &settings->length,
				settings->exclude, &settings->indim, settings->column,
				settings->dimset, settings->verbosity);
	}
	__android_log_print(ANDROID_LOG_DEBUG, "HUMANSENSE",
			"\tLength: %d\n\tEmbed Dim: %d\n", settings->length,
			settings->embdim);

	check_alloc(data = (ANNcoord*) calloc((settings->length - maxemb)
			* settings->embdim, sizeof(ANNcoord)));
	uint step = settings->embdim;
	for (i = maxemb; i < settings->length; i++) {
		rundel = 0;
		for (j = 0; j < settings->indim; j++) {
			emb = formatlist[j];
			for (k = 0; k < emb; k++)
				data[(i - maxemb) * step + (emb - k - 1)]
						= (ANNcoord) series[j][i - inddelay[rundel++]];
		}
	}
	length = settings->length - maxemb;

	for (j = 0; j < settings->indim; j++) {
		free(series[j]);
	}
	free(series);
	free(formatlist);
	free(inddelay);

}

void get_ann_points(ANNpointArray &dataPts, ANNcoord* series,
		unsigned long rows, uint cols) {
	unsigned long k = 0;
	dataPts = annAllocPts(rows, cols);
	for (ulong i = 0; i < rows; i++) {
		for (ulong j = 0; j < cols; j++) {
			dataPts[i][j] = series[k++];
		}
	}
}

void convert_to_ann_points(ANNpointArray &dataPts, ANNcoord* series, uint rows,
		uint cols) {
	uint k = 0, i, j;

	// Assume Allocated
	for (i = 0; i < rows; i++) {
		for (j = 0; j < cols; j++) {
			dataPts[i][j] = series[k++];
		}
	}
}

void print_matrix(CvMat* matrix, FILE *f) {
	int i, j;
	for (i = 0; i < matrix->rows; i++) {
		fprintf(f, FLOAT_OUT, CV_MAT_ELEM(*matrix, ANNcoord, i, 0));
		for (j = 1; j < matrix->cols; j++) {
			fprintf(f, " " FLOAT_OUT, CV_MAT_ELEM(*matrix, ANNcoord, i, j));
		}
		fprintf(f,"\n");
	}
}

void check_alloc(void *pointer) {
	if (pointer == NULL) {
		__android_log_print(ANDROID_LOG_DEBUG, "HUMANSENSE",
				"check_alloc: Couldn't allocate enough memory. Exiting\n");
		exit( CHECK_ALLOC_NOT_ENOUGH_MEMORY);
	}
}

#define SIZE_STEP 1000
double **get_multi_series(char *name, unsigned long *l, unsigned long ex,
		unsigned int *col, char *which, char colfix, unsigned int verbosity) {
	char *input, **format;
	int i, j;
	unsigned int *hcol, maxcol = 0, colcount = 0;
	unsigned long count, max_size = SIZE_STEP, hl, allcount;
	int input_size = INPUT_SIZE;
	double **x;
	FILE *fin;

	if (strlen(which) > 0) {
		colcount = 1;
		for (i = 0; i < strlen(which) - 1; i++) {
			if (!isdigit((unsigned int) which[i]) && (which[i] != ',')) {
				__android_log_print(ANDROID_LOG_ERROR, "HUMANSENSE",
						"Wrong format in the column string. Has to be num,num,num,...,num");
				return NULL;
			}
			if (which[i] == ',') {
				colcount++;
				which[i] = ' ';
			}
		}
		if (!isdigit((unsigned int) which[strlen(which) - 1])) {
			__android_log_print(ANDROID_LOG_ERROR, "HUMANSENSE",
					"Wrong format in the column string. Has to be num,num,num,...,num");
			return NULL;
		}
	}
	if (!colfix && (*col < colcount)) {
		*col = colcount;
	}

	check_alloc(input = (char*) calloc((size_t) input_size, (size_t) 1));
	check_alloc(hcol = (unsigned int*) malloc(sizeof(unsigned int) * *col));
	while ((int) (*which) && isspace((unsigned int) (*which))) {
		which++;
	}
	if (*which) {
		for (i = 0; i < *col - 1; i++) {
			sscanf(which, "%u", &hcol[i]);
			if (hcol[i] > maxcol) {
				maxcol = hcol[i];
			}
			while ((int) (*which) && !isspace((unsigned int) (*which))) {
				which++;
			}
			while ((int) (*which) && isspace((unsigned int) (*which))) {
				which++;
			}
			if (!((int) (*which))) {
				break;
			}
		}
	}
	else {
		i = -1;
	}

	if (*which) {
		sscanf(which, "%u", &hcol[i]);
	}
	else {
		for (j = i + 1; j < *col; j++) {
			hcol[j] = ++maxcol;
		}
	}
	check_alloc(format = (char**) malloc(sizeof(char*) * *col));
	for (i = 0; i < *col; i++) {
		check_alloc(format[i] = (char*) calloc((size_t)(4 * hcol[i]), (size_t) 1));
		strcpy(format[i], "");
		for (j = 1; j < hcol[i]; j++) {
			strcat(format[i], "%*lf");
		}
		strcat(format[i], "%lf");
	}
	free(hcol);

	check_alloc(x = (double**) malloc(sizeof(double*) * *col));
	for (i = 0; i < *col; i++) {
		check_alloc(x[i] = (double*) malloc(sizeof(double) * max_size));
	}
	hl = *l;

	count = 0;
	allcount = 0;
	if (name == NULL) {
		for (i = 0; i < ex; i++) {
			if ((input = getline(input, &input_size, stdin, verbosity)) == NULL) {
				break;
			}
		}
		while ((count < hl) && ((input = getline(input, &input_size, stdin,
				verbosity)) != NULL)) {
			if (count == max_size) {
				max_size += SIZE_STEP;
				for (i = 0; i < *col; i++) {
					check_alloc(x[i] = (double*) realloc(x[i], sizeof(double)
							* max_size));
				}
			}
			allcount++;
			for (i = 0; i < *col; i++) {
				if (sscanf(input, format[i], &x[i][count]) != 1) {
					break;
				}
			}
			if (i == *col) {
				count++;
			}
		}
	} else {
		fin = fopen(name, "r");
		for (i = 0; i < ex; i++) {
			if ((input = getline(input, &input_size, fin, verbosity)) == NULL) {
				break;
			}
		}
		while ((count < hl) && ((input = getline(input, &input_size, fin,
				verbosity)) != NULL)) {
			if (count == max_size) {
				max_size += SIZE_STEP;
				for (i = 0; i < *col; i++)
					check_alloc(x[i] = (double*) realloc(x[i], sizeof(double)
							* max_size));
			}
			allcount++;
			for (i = 0; i < *col; i++) {
				if (sscanf(input, format[i], &x[i][count]) != 1) {
					break;
				}
			}
			if (i == *col) {
				count++;
			}
		}
		fclose(fin);
	}

	for (i = 0; i < *col; i++) {
		free(format[i]);
	}
	free(format);
	free(input);

	*l = count;
	if (*l == 0) {
		__android_log_print(ANDROID_LOG_ERROR, "HUMANSENSE", "0 lines read.");
		return NULL;
	}
	if (max_size > count) {
		for (i = 0; i < *col; i++) {
			check_alloc(x[i] = (double*) realloc(x[i], sizeof(double) * count));
		}
	}

	return x;
}
char* getline(char *str, int *size, FILE *fin, unsigned int verbosity) {
	char *ret;
	char *hstr = NULL;
	char last;

	ret = fgets(str, *size, fin);
	if (ret == NULL)
		return NULL;

	last = str[strlen(str) - 1];

	while (last != '\n') {
		// If the line is too long, increase the length and read more data.
		*size += INPUT_SIZE;
		check_alloc(hstr = (char*) calloc((size_t) INPUT_SIZE, (size_t) 1));
		check_alloc(str = (char*) realloc(str, (size_t) * size));
		ret = fgets(hstr, INPUT_SIZE, fin);
		strcat(str, hstr);
		last = str[strlen(str) - 1];
		free(hstr);
	}
	return str;
}

#undef SIZE_STEP
