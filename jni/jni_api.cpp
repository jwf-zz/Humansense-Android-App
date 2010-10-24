/*
 * jni_api.cpp
 *
 *  Created on: 15-Sep-2009
 *      Author: jfrank8
 */


#include <ANN/ANNx.h>					// all ANN includes
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include "BuildTree.h"
#include "Classifier.h"
#include "ClassifyTrajectory.h"

// Sensor stuff
#include <hardware/sensors.h>
#include <cutils/native_handle.h>

#define NATIVE_CLASSIFIER_CALL(type, name) extern "C" JNIEXPORT type JNICALL Java_ca_mcgill_hs_classifiers_NativeClassifier_ ## name

extern Classifier *classifier;

NATIVE_CLASSIFIER_CALL(float, annDist)(JNIEnv* env, jobject obj, jint dim, jfloatArray p, jfloatArray q) {
	jfloat *pa, *qa, dist;
	jboolean isCopy = JNI_FALSE;
	pa = (jfloat*)env->GetPrimitiveArrayCritical(p, &isCopy);
	qa = (jfloat*)env->GetPrimitiveArrayCritical(q, &isCopy);
	if (!pa || !qa) return 0; // exception already pending.
	dist = annDist(dim, pa, qa);
	env->ReleasePrimitiveArrayCritical(p, pa, JNI_ABORT);
	env->ReleasePrimitiveArrayCritical(q, qa, JNI_ABORT);
	return dist;
}

NATIVE_CLASSIFIER_CALL(void, annClose)(JNIEnv* env, jobject obj) {
	annClose();
}

NATIVE_CLASSIFIER_CALL(void, classifyTrajectory)(JNIEnv* env, jobject obj, jstring inputFile, jstring outputFile, jstring modelsFile, jint numNeighbours, jint matchSteps) {
	const char *fin_name = env->GetStringUTFChars(inputFile, 0);
	const char *fout_name = env->GetStringUTFChars(outputFile, 0);
	const char *fmodel_name = env->GetStringUTFChars(modelsFile, 0);

	loadModels(fmodel_name, numNeighbours, matchSteps);
	classifyTrajectory(fin_name, fout_name);
	cleanUpModels();

	env->ReleaseStringUTFChars(inputFile, fin_name);
	env->ReleaseStringUTFChars(outputFile, fout_name);
	env->ReleaseStringUTFChars(modelsFile, fmodel_name);
}

NATIVE_CLASSIFIER_CALL(void, buildTree)(JNIEnv* env, jobject obj, jstring inputFile, jint m, jint p, jint d) {
	const char *in_file = env->GetStringUTFChars(inputFile, 0);

	buildTree(in_file, m, p, d);

	env->ReleaseStringUTFChars(inputFile, in_file);
}

NATIVE_CLASSIFIER_CALL(void, loadModels)(JNIEnv* env, jobject obj, jstring modelsFile, jint numNeighbours, jint matchSteps) {
	const char *fmodel_name = env->GetStringUTFChars(modelsFile, 0);

	loadModels(fmodel_name, numNeighbours, matchSteps);

	env->ReleaseStringUTFChars(modelsFile, fmodel_name);
}

NATIVE_CLASSIFIER_CALL(void, deleteModels)(JNIEnv* env, jobject obj) {
	cleanUpModels();
}

NATIVE_CLASSIFIER_CALL(jint, getNumModels)(JNIEnv* env, jobject obj) {
	if (classifier == NULL) {
		return 0;
	}
	return (jint)classifier->getNumModels();
}

NATIVE_CLASSIFIER_CALL(jint, getWindowSize)(JNIEnv* env, jobject obj) {
	if (classifier == NULL) {
		return 0;
	}
	return (jint)classifier->getWindowSize();
}

NATIVE_CLASSIFIER_CALL(void, setAlgorithmNumber)(JNIEnv* env, jobject obj, jint algNum) {
	if (classifier != NULL) {
		classifier->setAlgorithmNumber(algNum);
	}

}

NATIVE_CLASSIFIER_CALL(jstring, getModelNames)(JNIEnv* env, jobject obj) {
	char* res;
	jstring ret;

	if (classifier == NULL) {
		ret = env->NewStringUTF("");
	}
	else {
		res = classifier->getModelNames();
		ret = env->NewStringUTF(res);
		free(res);
	}
	return ret;
}

NATIVE_CLASSIFIER_CALL(void, classifySample)(JNIEnv* env, jobject obj, jfloatArray in, jint startIndex, jfloatArray out) {
	int i, M, length;
	ANNcoord *sample, *output, **data;
	jboolean isCopy = JNI_FALSE;
	CvMat *probs;

	M = classifier->getNumModels();
	sample = (ANNcoord*)env->GetPrimitiveArrayCritical(in, &isCopy);
	if (!sample) return; // exception already pending.

	CvMat *dists;
	data = new ANNcoord*[M];
	for (i = 0; i < M; i++ ) {
		data[i] = classifier->getProjectedData(i, sample+startIndex, classifier->getMatchSteps()+1);
		/*
		__android_log_print(ANDROID_LOG_DEBUG, HS_TAG, "%G %G %G %G %G",
				data[i][0], data[i][1], data[i][2], data[i][3], data[i][4]);
		*/
	}

	probs = classifier->classify(data, classifier->getMatchSteps());
	// Use JNI_ABORT because this array was read-only
	env->ReleasePrimitiveArrayCritical(in, sample, JNI_ABORT);

	for (i = 0; i < M; i++) {
		delete [] data[i];
	}
	delete [] data;

	output = (jfloat*)env->GetPrimitiveArrayCritical(out, &isCopy);
	if (!output) return; // exception already pending

	for (i = 0; i < M; i++) {
		output[i] = CV_MAT_ELEM(*probs, ANNcoord, 0, i);
	}
	// Use 0 for mode flag because this array was modified.
	env->ReleasePrimitiveArrayCritical(out, output, 0);
	cvReleaseMat(&probs);
}
