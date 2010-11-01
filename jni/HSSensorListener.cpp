/*
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information
 *
 * HSSensorListener.cpp
 *
 */

#include <jni.h>
#include "Utils.h"

// Sensor stuff
#include <hardware/sensors.h>
#include <cutils/native_handle.h>

#define NATIVE_SENSOR_CALL(type, name) extern "C" JNIEXPORT type JNICALL Java_ca_mcgill_hs_hardware_Sensor_ ## name

static struct file_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
    jfieldID mDescriptor;
} gFileDescriptorOffsets;

static struct parcel_file_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
} gParcelFileDescriptorOffsets;

static struct bundle_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
    jmethodID mPutIntArray;
    jmethodID mPutParcelableArray;
} gBundleOffsets;

/*
 * The method below are not thread-safe and not intended to be
 */

static sensors_control_device_t* sSensorDevice = 0;

NATIVE_SENSOR_CALL(jint, androidInit)(JNIEnv *env, jclass clazz)
{
    sensors_module_t* module;
    if (hw_get_module(SENSORS_HARDWARE_MODULE_ID, (const hw_module_t**)&module) == 0) {
        if (sensors_control_open(&module->common, &sSensorDevice) == 0) {
            const struct sensor_t* list;
            int count = module->get_sensors_list(module, &list);
            return count;
        }
    }
    return 0;
}

/**
 * Returns an android.os.Bundle;
 */
NATIVE_SENSOR_CALL(jobject, androidOpen)(JNIEnv *env, jclass clazz)
{
    native_handle_t* handle = sSensorDevice->open_data_source(sSensorDevice);
    if (!handle) {
        return NULL;
    }

    // new Bundle()
    jobject bundle = env->NewObject(
            gBundleOffsets.mClass,
            gBundleOffsets.mConstructor);

    if (handle->numFds > 0) {
        jobjectArray fdArray = env->NewObjectArray(handle->numFds,
                gParcelFileDescriptorOffsets.mClass, NULL);
        for (int i = 0; i < handle->numFds; i++) {
            // new FileDescriptor()
            jobject fd = env->NewObject(gFileDescriptorOffsets.mClass,
                    gFileDescriptorOffsets.mConstructor);
            env->SetIntField(fd, gFileDescriptorOffsets.mDescriptor, handle->data[i]);
            // new ParcelFileDescriptor()
            jobject pfd = env->NewObject(gParcelFileDescriptorOffsets.mClass,
                    gParcelFileDescriptorOffsets.mConstructor, fd);
            env->SetObjectArrayElement(fdArray, i, pfd);
        }
        // bundle.putParcelableArray("fds", fdArray);
        env->CallVoidMethod(bundle, gBundleOffsets.mPutParcelableArray,
                env->NewStringUTF("fds"), fdArray);
    }

    if (handle->numInts > 0) {
        jintArray intArray = env->NewIntArray(handle->numInts);
        env->SetIntArrayRegion(intArray, 0, handle->numInts, &handle->data[handle->numInts]);
        // bundle.putIntArray("ints", intArray);
        env->CallVoidMethod(bundle, gBundleOffsets.mPutIntArray,
                env->NewStringUTF("ints"), intArray);
    }

    // delete the file handle, but don't close any file descriptors
    native_handle_delete(handle);
    return bundle;
}

NATIVE_SENSOR_CALL(jint, androidClose)(JNIEnv *env, jclass clazz)
{
    if (sSensorDevice->close_data_source)
        return sSensorDevice->close_data_source(sSensorDevice);
    else
        return 0;
}

NATIVE_SENSOR_CALL(jboolean, androidActivate)(JNIEnv *env, jclass clazz, jint sensor, jboolean activate)
{
    int active = sSensorDevice->activate(sSensorDevice, sensor, activate);
    return (active<0) ? false : true;
}

NATIVE_SENSOR_CALL(jint, androidSetDelay)(JNIEnv *env, jclass clazz, jint ms)
{
    return sSensorDevice->set_delay(sSensorDevice, ms);
}

NATIVE_SENSOR_CALL(jint, androidDataWake)(JNIEnv *env, jclass clazz)
{
    int res = sSensorDevice->wake(sSensorDevice);
    return res;
}
