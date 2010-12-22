JNI_DIR := $(call my-dir)
LOCAL_PATH := $(JNI_DIR)
include $(CLEAR_VARS)

include $(call all-makefiles-under,$(LOCAL_PATH))

LOCAL_PATH := $(JNI_DIR)

LOCAL_SRC_FILES := \
	BuildTree.cpp \
	Classifier.cpp \
	ClassifyTrajectory.cpp \
	jni_api.cpp \
	TDEModel.cpp \
	Utils.cpp

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/ANN

LOCAL_STATIC_LIBRARIES := libANN libcxcore

LOCAL_MODULE:= humansense
LOCAL_MODULE_FILENAME:= libhumansense

include $(BUILD_SHARED_LIBRARY)
