root-dir:=$(APP_PROJECT_PATH)

BUILD_AMR=light

WITH_OPENSSL=1
ENABLE_PHONE_ENGINE=0
ENABLE_CHAT=0
BUILD_X264=0

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
include $(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/android/build/ffmpeg/Android.mk
endif

include $(root-dir)/submodules/vtalk/Android.mk

include $(VUECE_LIBJINGLE_LOCATION)/libjingle/Android.mk
include $(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/expat/Android.mk
include $(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/jthread/Android.mk
include $(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/coffeecatch/Android.mk