LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../ \
	$(LOCAL_PATH)/../../ext/astl/include/ \
	$(LOCAL_PATH)/../../ext/bionic/libstdc++/include/

LOCAL_MODULE    := libcxcore
LOCAL_LDLIBS    := -llog

LOCAL_SRC_FILES := \
	cxjacobieigens.cpp \
	cxdrawing.cpp \
	cxnorm.cpp \
	cxdxt.cpp \
	cxmean.cpp \
	cxsumpixels.cpp \
	cxerror.cpp \
	cxswitcher.cpp \
	cxconvert.cpp \
	cxrand.cpp \
	cxarithm.cpp \
	cxlut.cpp \
	cxouttext.cpp \
	cxcopy.cpp \
	cxlogic.cpp \
	cxprecomp.cpp \
	cxmatmul.cpp \
	cxmathfuncs.cpp \
	cxminmaxloc.cpp \
	cxalloc.cpp \
	cxmeansdv.cpp \
	cxarray.cpp \
	cxutils.cpp \
	cxdatastructs.cpp \
	cxpersistence.cpp \
	cxsvd.cpp \
	cximage.cpp \
	cxcmp.cpp \
	cxtables.cpp \
	cxmatrix.cpp

include $(BUILD_STATIC_LIBRARY)
