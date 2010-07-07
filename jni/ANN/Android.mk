LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../ \
	$(LOCAL_PATH)/../../ext/astl/include/ \
	$(LOCAL_PATH)/../../ext/bionic/libstdc++/include/

LOCAL_MODULE    := libANN
LOCAL_LDLIBS    := -llog

LOCAL_SRC_FILES := \
	ANN.cpp \
	bd_pr_search.cpp \
	bd_tree.cpp \
	kd_pr_search.cpp \
	kd_split.cpp \
	kd_util.cpp \
	bd_fix_rad_search.cpp \
	bd_search.cpp \
	brute.cpp \
	kd_fix_rad_search.cpp \
	kd_search.cpp \
	kd_tree.cpp \
	kd_dump.cpp \
	perf.cpp

include $(BUILD_STATIC_LIBRARY)
