/*
 * HSSensorManager.cpp
 *
 *  Created on: 03-Sep-2010
 *      Author: jfrank8
 */

#include <jni.h>
#include "Utils.h"

// Sensor stuff
#include <hardware/sensors.h>
#include <cutils/native_handle.h>

#define NATIVE_SENSOR_CALL(type, name) extern "C" JNIEXPORT type JNICALL Java_ca_mcgill_hs_hardware_Sensor_ ## name

struct SensorOffsets
{
    jfieldID    name;
    jfieldID    vendor;
    jfieldID    version;
    jfieldID    handle;
    jfieldID    type;
    jfieldID    range;
    jfieldID    resolution;
    jfieldID    power;
} gSensorOffsets;

static sensors_module_t* sSensorModule = 0;
static sensors_data_device_t* sSensorDevice = 0;

NATIVE_SENSOR_CALL(void, nativeClassInit)(JNIEnv *_env, jclass _this) {
    jclass sensorClass = _env->FindClass("ca/mcgill/hs/hardware/Sensor");
    SensorOffsets& sensorOffsets = gSensorOffsets;
    sensorOffsets.name        = _env->GetFieldID(sensorClass, "mName",      "Ljava/lang/String;");
    sensorOffsets.vendor      = _env->GetFieldID(sensorClass, "mVendor",    "Ljava/lang/String;");
    sensorOffsets.version     = _env->GetFieldID(sensorClass, "mVersion",   "I");
    sensorOffsets.handle      = _env->GetFieldID(sensorClass, "mHandle",    "I");
    sensorOffsets.type        = _env->GetFieldID(sensorClass, "mType",      "I");
    sensorOffsets.range       = _env->GetFieldID(sensorClass, "mMaxRange",  "F");
    sensorOffsets.resolution  = _env->GetFieldID(sensorClass, "mResolution","F");
    sensorOffsets.power       = _env->GetFieldID(sensorClass, "mPower",     "F");
}

NATIVE_SENSOR_CALL(jint, sensorsModuleInit)(JNIEnv *env, jclass clazz) {
	int err = 0;
	sensors_module_t const* module;
	err = hw_get_module(SENSORS_HARDWARE_MODULE_ID, (const hw_module_t **)&module);
	if (err == 0)
			sSensorModule = (sensors_module_t*)module;
	return err;
}

NATIVE_SENSOR_CALL(jint, sensorsDataInit)(JNIEnv *env, jclass clazz)
{
    if (sSensorModule == NULL)
        return -1;
    int err = sensors_data_open(&sSensorModule->common, &sSensorDevice);
    return err;
}

NATIVE_SENSOR_CALL(jint, sensorsDataUninit)(JNIEnv *env, jclass clazz) {
	int err = 0;
	if (sSensorDevice) {
		err = sensors_data_close(sSensorDevice);
		if (err == 0) {
			sSensorDevice = 0;
		}
	}
	return err;
}

NATIVE_SENSOR_CALL(jint, sensorsDataOpen)(JNIEnv *env, jclass clazz, jobjectArray fdArray, jintArray intArray)
{
    jclass FileDescriptor = env->FindClass("java/io/FileDescriptor");
    jfieldID fieldOffset = env->GetFieldID(FileDescriptor, "descriptor", "I");
    int numFds = (fdArray ? env->GetArrayLength(fdArray) : 0);
    int numInts = (intArray ? env->GetArrayLength(intArray) : 0);
    native_handle_t* handle = native_handle_create(numFds, numInts);
    int offset = 0;

    for (int i = 0; i < numFds; i++) {
        jobject fdo = env->GetObjectArrayElement(fdArray, i);
        if (fdo) {
            handle->data[offset++] = env->GetIntField(fdo, fieldOffset);
        } else {
            handle->data[offset++] = -1;
        }
    }
    if (numInts > 0) {
        jint* ints = env->GetIntArrayElements(intArray, 0);
        for (int i = 0; i < numInts; i++) {
            handle->data[offset++] = ints[i];
        }
        env->ReleaseIntArrayElements(intArray, ints, 0);
    }

    // doesn't take ownership of the native handle
    return sSensorDevice->data_open(sSensorDevice, handle);
}

NATIVE_SENSOR_CALL(jint, sensorsDataClose)(JNIEnv *env, jclass clazz)
{
    return sSensorDevice->data_close(sSensorDevice);
}

NATIVE_SENSOR_CALL(jint, sensorsModuleGetNextSensor)(JNIEnv *env, jobject clazz, jobject sensor, jint next)
{
    if (sSensorModule == NULL)
        return 0;

    HS_LOG("HERE 1");

    SensorOffsets& sensorOffsets = gSensorOffsets;
    const struct sensor_t* list;
    int count = sSensorModule->get_sensors_list(sSensorModule, &list);
    if (next >= count)
        return -1;

    list += next;

    jstring name = env->NewStringUTF(list->name);
    jstring vendor = env->NewStringUTF(list->vendor);
    env->SetObjectField(sensor, sensorOffsets.name,      name);
    env->SetObjectField(sensor, sensorOffsets.vendor,    vendor);
    env->SetIntField(sensor, sensorOffsets.version,      list->version);
    env->SetIntField(sensor, sensorOffsets.handle,       list->handle);
    env->SetIntField(sensor, sensorOffsets.type,         list->type);
    env->SetFloatField(sensor, sensorOffsets.range,      list->maxRange);
    env->SetFloatField(sensor, sensorOffsets.resolution, list->resolution);
    env->SetFloatField(sensor, sensorOffsets.power,      list->power);

    next++;
    return next<count ? next : 0;
}

NATIVE_SENSOR_CALL(jint, sensorsDataPoll)(JNIEnv *env, jclass clazz,
        jfloatArray values, jintArray status, jlongArray timestamp)
{
    sensors_data_t data;
    int res = sSensorDevice->poll(sSensorDevice, &data);
    if (res >= 0) {
        jint accuracy = data.vector.status;
        env->SetFloatArrayRegion(values, 0, 3, data.vector.v);
        env->SetIntArrayRegion(status, 0, 1, &accuracy);
        env->SetLongArrayRegion(timestamp, 0, 1, &data.time);
    }
    return res;
}
