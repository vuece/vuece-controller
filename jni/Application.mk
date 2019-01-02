
APP_PROJECT_PATH := $(call my-dir)/../

VUECE_LIBJINGLE_LOCATION := $(call my-dir)/../../vuece-libjingle
VUECE_LIBJINGLE_SRC_LOCATION := $(call my-dir)/../../vuece-libjingle/libjingle
VUECE_LIBJINGLE_EXTERNALS_LOCATION := $(call my-dir)/../../vuece-libjingle/externals

VUECE_ENABLE_VIDEO := 0

APP_MODULES      :=libvtalk

ifeq ($(WITH_OPENSSL),1)
APP_MODULES += libcrypto libssl
endif


ifeq ($(RING),yes)
APP_MODULES      += libring
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
APP_MODULES      += libavcodec libavutil libavformat libavcore libswscale
endif

APP_BUILD_SCRIPT:=$(call my-dir)/Android.mk
APP_PLATFORM := android-8
APP_ABI := armeabi-v7a

#ndk8
APP_STL := gnustl_static

APP_CFLAGS += -Wno-deprecated-declarations -Wno-error=format-security

#####################################
# NOTE: DO NOT use the following debug option to compile mediastreamer2, this causes error when
# compiling scaler.c !!!!!!!!!!!!! So DO NOT USES IT!
########################
#APP_OPTIM := debug

