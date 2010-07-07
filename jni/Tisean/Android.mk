LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../ \
	$(LOCAL_PATH)/../../ext/astl/include/ \
	$(LOCAL_PATH)/../../ext/bionic/libstdc++/include/

LOCAL_MODULE    := libTisean
LOCAL_LDLIBS    := -llog

LOCAL_SRC_FILES := \
	check_option.c \
	rand.c \
	scan_help.c \
	get_multi_series.c \
	find_neighbors.c \
	rand_arb_dist.c \
	solvele.c \
	myfgets.c \
	make_box.c \
	exclude_interval.c \
	invert_matrix.c \
	make_multi_box.c \
	eigen.c \
	find_multi_neighbors.c \
	test_outfile.c \
	make_multi_index.c \
	make_multi_box2.c \
	what_i_do.c \
	search_datafile.c \
	variance.c \
	get_series.c \
	rescale_data.c \
	check_alloc.c

include $(BUILD_STATIC_LIBRARY)
