LOCAL_PATH := $(call my-dir)

VUECE_LIBJINGLE_CLIENT_CORE := ../../../vuece-libjingle/client-core
VUECE_OPENSSL_LIB:= ../../../vuece-libjingle/externals/android/openssl/lib

include $(CLEAR_VARS)
LOCAL_MODULE:=libssl
LOCAL_SRC_FILES:=$(VUECE_OPENSSL_LIB)/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:=libcrypto
LOCAL_SRC_FILES:=$(VUECE_OPENSSL_LIB)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
common_CFLAGS := \
		-DANDROID \
		-DPOSIX \
		-DLINUX \
		-DXML_STATIC \
		-DFEATURE_ENABLE_SSL \
		-DFEATURE_ENABLE_VOICEMAIL \
		-DSSL_USE_OPENSSL \
		-DHAVE_OPENSSL_SSL_H \
		-DOPENSSL_NO_X509 \
		-DSRTP_RELATIVE_PATH \
		-DEXPAT_RELATIVE_PATH \
		-D__STDC_CONSTANT_MACROS \
		-DHAVE_PTHREAD=1 \
		-DHAVE_MMX=1 \
		-D__SSE__ \
		-DARCH_X86=1 \
		-g \
		-DVUECE_APP_ROLE_HUB_CLIENT \
		-DLOGGING=1 

MY_LIBJINGLE_INCLUDES := \
	$(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/expat \
	$(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/jthread/src \
	$(VUECE_LIBJINGLE_SRC_LOCATION) \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/base \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/xmpp \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/xmllite \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/session \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/session/phone \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/session/phone/vuece \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/talk/session/fileshare \
	$(VUECE_LIBJINGLE_SRC_LOCATION)/p2p/base \
	$(VUECE_LIBJINGLE_LOCATION)/client-core \
	$(VUECE_LIBJINGLE_EXTERNALS_LOCATION)/android/openssl/include

MY_JINGLE_CALLER_INCLUDES := \
	$(LOCAL_PATH)/../../../vuece-libjingle/client-core \
	$(LOCAL_PATH)/jingle \
	$(LOCAL_PATH)/jingle/client/android 
	
	
LOCAL_CPP_EXTENSION := .cc

LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_C_INCLUDES := $(MY_LIBJINGLE_INCLUDES)
LOCAL_C_INCLUDES += $(MY_JINGLE_CALLER_INCLUDES)


LOCAL_MODULE    := vtalk

MY_SRC_FILES_CALL_CLIENT = \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceGlobalContext.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceCoreClient.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceKernelShell.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/discoitemsquerytask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/friendinvitesendtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/rosterquerysendtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/rosterqueryresultrecvtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/RosterSubResponseRecvTask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/mucinviterecvtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/mucinvitesendtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/presenceouttask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/presencepushtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/chatpushtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/vcardpushtask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/jingleinfotask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/vhubgettask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/vhubresulttask.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceConnectionKeeper.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceNativeInterface.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceCommon.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/DebugLog.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceLogger.cc \
$(VUECE_LIBJINGLE_CLIENT_CORE)/VueceThreadUtil.cc \
jingle/client/android/VueceNativeClientImplAndroid.cc

#$(VUECE_LIBJINGLE_CLIENT_CORE)/voicemailjidrequester.cc \

LOCAL_SRC_FILES := jingle/client/android/vtalk.cc
LOCAL_SRC_FILES += $(MY_SRC_FILES_CALL_CLIENT)

# for logging
LOCAL_LDLIBS += -llog -ldl

#NOTE: DO NOT CHANGE THE ORDER !
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_STATIC_LIBRARIES := \
libjingle \
libexpat \
libssl \
libcrypto \
libjthread \
libavformat \
libavcodec \
libavcore \
libswscale \
libavutil
else
LOCAL_STATIC_LIBRARIES := \
libjingle \
libexpat \
libssl \
libcrypto \
libjthread
endif

LOCAL_STATIC_LIBRARIES += cpufeatures

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DVIDEO_ENABLED=0
LOCAL_CFLAGS += -DHAVE_AVCODEC
endif

LOCAL_CFLAGS += -funwind-tables -Wl,--no-merge-exidx-entries
LOCAL_LDFLAGS	:= -Wl

$(info ---------- Vuece Build Enviroment(vtalk) ------------)
$(info TARGET_ARCH_ABI = $(TARGET_ARCH_ABI))
$(info ----------)
$(info LOCAL_LDLIBS = $(LOCAL_LDLIBS))
$(info ----------)
$(info LOCAL_CFLAGS = $(LOCAL_CFLAGS))
$(info ----------)
$(info LOCAL_LDFLAGS = $(LOCAL_LDFLAGS))
$(info ----------)
$(info LOCAL_STATIC_LIBRARIES = $(LOCAL_STATIC_LIBRARIES))


LOCAL_MODULE_CLASS = SHARED_LIBRARIES

LOCAL_ALLOW_UNDEFINED_SYMBOLS := false

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)

include $(BUILD_THIRD_PARTY_PACKAGE)
