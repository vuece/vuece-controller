
#include "VueceNativeInterface.h"
#include "com_vuece_vtalk_android_jni_JabberClient.h"
#include "vuecelogging.h"
#include "talk/base/logging.h"
#include "VueceGlobalSetting.h"
#include "talk/session/fileshare/VueceJni.h"

using namespace vuece;

#define MAIN_THREAD_NAME "VTALK"

VueceNativeInterface* native_client;

JavaVM *android_jvm;

static InitData init_data;
static unsigned int current_thread_key;

int CreateVueceStreamer(int, int);

const char* DescribeStatus(buzz::Status::Show show, const std::string& desc) {

	LOGD("vtalk - DescribeStatus\n");

	switch (show) {
	case buzz::Status::SHOW_XA:
		return desc.c_str();
	case buzz::Status::SHOW_ONLINE:
		return "online";
	case buzz::Status::SHOW_AWAY:
		return "away";
	case buzz::Status::SHOW_DND:
		return "do not disturb";
	case buzz::Status::SHOW_CHAT:
		return "ready to chat";
	default:
		return "offline";
	}
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;

	//TODO Maybe we log our DL version number here
	LOGD("------------------------------------------------");
	LOGD("vtak - JNI_OnLoad: JNI_VERSION_1_2");
	LOGD("------------------------------------------------");
	android_jvm = vm;
	//Assume it is c++
	//vm->GetEnv ((void **) &env, JNI_VERSION_1_2);

	native_client = NULL;

	VueceJni::SetJvmForCurrentThread(vm, &current_thread_key, MAIN_THREAD_NAME);

    return JNI_VERSION_1_2;
}

static jlong InternalInitiate(JNIEnv * env, jobject thiz,
		jobject alistener, jint loggingLevel,
		jstring device_name, jstring app_version)
{
	LOGD("vtalk - InternalInitiate\n");

	if(native_client != NULL)
	{
		LOG(WARNING) << "Calling initiate but native_client instance is not empty!";
		return (jlong)-1;
	}

	memset(&init_data, 0, sizeof(InitData));

	const char *dev_name = env->GetStringUTFChars(device_name,0);
	const char *app_ver = env->GetStringUTFChars(app_version,0);

	init_data.p0 = android_jvm;
	init_data.p1 = env;
	init_data.p2 = &alistener;
	init_data.p3 = (void*)dev_name;
	init_data.p4 = (void*)app_ver;
	init_data.logging_level = (int)loggingLevel;

	native_client = VueceNativeInterface::CreateInstance(&init_data);

	VueceJni::AttachCurrentThreadToJniEnv(MAIN_THREAD_NAME);

	env->ReleaseStringUTFChars(device_name, dev_name);
	env->ReleaseStringUTFChars(app_version, app_ver);

	return (jlong)native_client;
}

//DDD
JNIEXPORT jlong JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_initiate
  (JNIEnv * env, jobject thiz, jobject alistener, jint loggingLevel,
		  jstring device_name, jstring app_version)
{

	jlong native_client = 0;

	LOGD("vtalk - initiate\n");

	native_client = InternalInitiate(env, thiz, alistener, loggingLevel, device_name, app_version);

	return (jlong)native_client;
}

static jint InternalGetState(JNIEnv * env, jobject thiz)
{
	if(native_client != NULL)
	{
		return native_client->GetClientState();
	}
	else
	{
		LOG(INFO) << "InternalGetState - client instance is null, return Offline";

		return (jint)CLIENT_STATE_OFFLINE;
	}

}

JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getClientState
  (JNIEnv * env, jobject thiz)
{
	jint ret = -1;
	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_getClientState";

	ret = InternalGetState(env, thiz);

	return ret;
}

static jint InternalGetNetworkPlayerState(JNIEnv * env, jobject thiz)
{
	if(native_client != NULL)
	{
		return native_client->GetNetworkPlayerState();
	}
	else
	{
		LOG(INFO) << "InternalGetNetworkPlayerState - client instance is null, return NetworkPlayerState_None";
	}
}

JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getNetworkPlayerState
  (JNIEnv * env, jobject thiz)
{
	jint ret = -1;

	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_getNetworkPlayerState";

	ret = InternalGetNetworkPlayerState(env, thiz);

	return ret;

}

static jboolean InternalIsMusicStreaming(JNIEnv * env, jobject thiz)
{
	if(native_client != NULL)
	{
		if (native_client->IsMusicStreaming())
			return JNI_TRUE;
	}
	else
	{
		LOG(INFO)  << "InternalIsMusicStreaming - client instance is null, return JNI_FALSE";
	}

	return JNI_FALSE;

}


JNIEXPORT jboolean JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_isMusicStreaming
  (JNIEnv * env, jobject thiz)
{
	jboolean b = JNI_FALSE;
	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_isMusicStreaming";

	b = InternalIsMusicStreaming(env, thiz);

	return b;
}

static jint InternalGetCurrentMusicStreamingProgress(JNIEnv * env, jobject thiz)
{
	if(native_client != NULL)
	{	//share id is empty for now
		return (jint) native_client->GetCurrentMusicStreamingProgress("");
	}
	else
	{
		LOG(INFO)  << "InternalGetCurrentMusicStreamingProgress - client instance is null, return 0";
	}

	return 0;

}


JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getCurrentMusicStreamingProgress
  (JNIEnv * env, jobject thiz)
{
	jint p = -1;
	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_getCurrentMusicStreamingProgress";

	p = InternalGetCurrentMusicStreamingProgress(env, thiz);
	return p;
}

static jint InternalGetCurrentPlayingProgress(JNIEnv * env, jobject thiz)
{
	if(native_client != NULL)
	{
		//share id is NULL for now
		return native_client->GetCurrentPlayingProgress();
	}
	else
	{
		LOG(INFO)  << "InternalGetCurrentPlayingProgress - client instance is null, return 0";
	}

	return 0;
}


JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getCurrentPlayingProgress
  (JNIEnv * env, jobject thiz)
{
	jint p = -1;

	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_getCurrentPlayingProgress";
	//share id is NULL for now
//	return native_client->GetCurrentPlayingProgress();

	p = InternalGetCurrentPlayingProgress(env, thiz);
	return p;

}

static jint InternalStart(JNIEnv * env, jobject thiz, jstring username_str, jstring password_str, jint auth_type)
{
	const char *username = env->GetStringUTFChars(username_str,0);
	const char *pwd = env->GetStringUTFChars(password_str,0);

	bool bIsLoginFailed = true;

	LOGD("vtalk - InternalStart, username = %s\n", username);

	if(native_client == NULL)
	{
		LOG(LS_INFO) << "InternalStart - Client instance is null, recreate client instance now.";

		native_client = VueceNativeInterface::CreateInstance(&init_data);
	}

    // this is a blocked call.
	bIsLoginFailed = native_client->Start(username, pwd, (int)auth_type);

	if(bIsLoginFailed)
	{
		LOGD("vtalk - InternalStart, login failed: username = %s\n", username);
	}

	LOG(INFO) << "vtak.cc:Deleting native_client instance";

	delete native_client;
	native_client = NULL;

	LOG(INFO) << "vtak.cc: native_client deleted 1a";

	env->ReleaseStringUTFChars(username_str, username);
	env->ReleaseStringUTFChars(password_str, pwd);

	LOG(INFO) << "vtak.cc: native_client deleted 2a -- Deletion done";

    return 0;
}



JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_start
  (JNIEnv * env, jobject thiz, jstring username_str, jstring password_str, jint auth_type)
{

	int r = 0;

	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_start - start";

	//don't why this causes crash during logout, this can be reproduced by disabling
	//the phone's wifi module
	//COFFEE_TRY_JNI(env, InternalStart(env, thiz, username_str, password_str, auth_type));

	r  = InternalStart(env, thiz, username_str, password_str, auth_type);

	LOG(INFO) << "Java_com_vuece_vtalk_android_jni_JabberClient_start - done";
	return 0;
}


static jint InternalLogout(JNIEnv * env, jobject thiz)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalLogout - Client instance is null, sth is wrong.";
		return -1;
	}

	//share id is NULL for now
	native_client->LogOut();

	return 0;
}


JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_logout
  (JNIEnv * env, jobject thiz)
{
	int r = -1;
	LOGD("vtalk - logout\n");

	r = InternalLogout(env, thiz);

	return r;
}

JNIEXPORT jint 		JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendChat
  (JNIEnv * env, jobject thiz, jstring jid_str, jstring message_str)
{
	LOGD("vtalk - sendChat\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "Java_com_vuece_vtalk_android_jni_JabberClient_sendChat - Client instance is null, sth is wrong.";
		return -1;
	}



#ifdef CHAT_ENABLED
	const char *jid = env->GetStringUTFChars(jid_str,0);
	const char *message = env->GetStringUTFChars(message_str,0);
	std::string s_jid(jid);
	std::string s_message(message);
	native_client->SendChat(s_jid,s_message);
	env->ReleaseStringUTFChars(jid_str, jid);
	env->ReleaseStringUTFChars(message_str, message);
#endif
}

JNIEXPORT jint 		JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendVCardRequest
	(JNIEnv * env, jobject thiz, jstring jid_str)
{
	LOGD("vtalk - sendVCardRequest\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "sendVCardRequest - Client instance is null, sth is wrong.";
		return -1;
	}


#ifdef VCARD_ENABLED
	const char *jid = env->GetStringUTFChars(jid_str,0);
	std::string s_jid(jid);
	native_client->SendVCardRequest(s_jid);
	env->ReleaseStringUTFChars(jid_str, jid);
#endif
}

static jint InternalSendVHubMessage
(JNIEnv * env, jobject thiz, jstring jid_str, jstring type_str, jstring message_str)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalSendVHubMessage - Client instance is null, sth is wrong.";


		return -1;
	}
	else
	{
		const char *jid = env->GetStringUTFChars(jid_str,0);
		std::string s_jid(jid);
		const char *type = env->GetStringUTFChars(type_str,0);
		std::string s_type(type);
		const char *message = env->GetStringUTFChars(message_str,0);
		std::string s_message(message);

		LOGD("vtalk - sendVHubMessage\n");

		native_client->SendVHubMessage(s_jid, s_type, s_message);
		env->ReleaseStringUTFChars(jid_str, jid);
		env->ReleaseStringUTFChars(type_str, type);
		env->ReleaseStringUTFChars(message_str, message);
	}

	return 0;
}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendVHubMessage
	(JNIEnv * env, jobject thiz, jstring jid_str, jstring type_str, jstring message_str)
{
	int r = -1;

	r = InternalSendVHubMessage(env, thiz, jid_str, type_str, message_str);

	return r;
}

static jint InternalSendVHubPlayRequest
(JNIEnv * env, jobject thiz, jstring jid_str, jstring type_str, jstring message_str, jstring target_uri_str)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalSendVHubPlayRequest - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *jid = env->GetStringUTFChars(jid_str,0);
		std::string s_jid(jid);
		const char *type = env->GetStringUTFChars(type_str,0);
		std::string s_type(type);
		const char *message = env->GetStringUTFChars(message_str,0);
		std::string s_message(message);
		const char *target_uri = env->GetStringUTFChars(target_uri_str,0);
		std::string s_uri(target_uri);

		LOGD("vtalk - sendVHubPlayRequest\n");

		native_client->SendVHubPlayRequest(s_jid, s_type, s_message, s_uri);

		env->ReleaseStringUTFChars(jid_str, jid);
		env->ReleaseStringUTFChars(type_str, type);
		env->ReleaseStringUTFChars(message_str, message);
		env->ReleaseStringUTFChars(target_uri_str, target_uri);

		return 0;
	}

}

JNIEXPORT jint 		JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendVHubPlayRequest
	(JNIEnv * env, jobject thiz, jstring jid_str, jstring type_str, jstring message_str, jstring target_uri_str)
{

	int r = -1;

	r = InternalSendVHubPlayRequest(env, thiz, jid_str, type_str, message_str, target_uri_str);

	return r;
}

JNIEXPORT jint 		JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_addBuddy
	(JNIEnv * env, jobject thiz, jstring jid_str)
{
	const char *jid = env->GetStringUTFChars(jid_str,0);
	std::string s_jid(jid);

	LOGD("vtalk - addBuddy\n");

	native_client->AddBuddy(s_jid);
	env->ReleaseStringUTFChars(jid_str, jid);

}

static jint InternalSendPresence
(JNIEnv * env, jobject thiz, jstring show_str, jstring status_str)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalSendPresence - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *status = env->GetStringUTFChars(status_str,0);
		std::string s_status(status);
		const char *show = env->GetStringUTFChars(show_str,0);
		std::string s_show(show);

		LOGD("vtalk - sendPresence\n");

		native_client->SendPresence(s_show, s_status);
		env->ReleaseStringUTFChars(status_str, status);
		env->ReleaseStringUTFChars(show_str, show);

		return 0;
	}

}


JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendPresence
	(JNIEnv * env, jobject thiz, jstring show_str, jstring status_str)
{

	int r = -1;

	r = InternalSendPresence(env, thiz, show_str, status_str);

	return r;
}

JNIEXPORT jobject 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getCallRequest
	(JNIEnv * env, jobject thiz)
{
    jmethodID constructorMethodID;
    jclass objCls = env->FindClass("com/vuece/vtalk/android/model/CallOption");

    LOGD("vtalk - getCallRequest\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "getCallRequest - Client instance is null, sth is wrong.";
		return NULL;
	}


    if (objCls == NULL) {
    	LOGD("cannot find CallOption class\n");
        return NULL; /* exception thrown */
    }

    constructorMethodID = env->GetMethodID(objCls,
    		"<init>",
    		"(Ljava/lang/String;ZZI)V");

    if (constructorMethodID == NULL) {
    	LOGD("NULL RETURNED TO constructorMethodID\n");
    	return NULL;
    }

    LOGD("Java_com_vuece_vtalk_android_jni_JabberClient_getCallRequest -> constructorMethodID = %d\n",(int)constructorMethodID);

    //get current call status here

    cricket::CallOptions* callOpt = native_client->GetCurrentCallOption();
    if(callOpt == NULL)
    {
    	LOGD("getCallRequest: callOpt is null");
    	return 0;
    }

    bool isVideo = true;
    bool isMuc = false;
    int bandWidth = 0;

    isVideo 	= callOpt->is_video;
    isMuc 		= callOpt->is_muc;
    bandWidth 	= callOpt->video_bandwidth;
    buzz::Jid  jid = native_client->GetCurrentJidInCall();

    const char* jid_s = jid.Str().c_str();

    jobject returnObj = env->NewObject(objCls, constructorMethodID, env->NewStringUTF(jid_s), (jboolean)isVideo, (jboolean)isMuc, (jint)bandWidth);

    return returnObj;
}

static jobjectArray InternalGetRoster
(JNIEnv * env, jobject thiz)
{
	jobjectArray result;
	jmethodID constructorMethodID;
	int i;
	jobject returnObj;
	int rosterSize = 0;
	RosterMap* rosterMap = NULL;

	LOGD("vtalk - getRoster\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalGetRoster - Client instance is null, sth is wrong.";
		return NULL;
	}


	jclass objCls = env->FindClass("com/vuece/vtalk/android/model/RosterEntry");

	if (objCls == NULL) {
		LOGD("cannot find RosterEntry class\n");
		return NULL; /* exception thrown */
	}
	/************************************************** ***********************
	// Allocates a new Java object without invoking any of the
	constructors
	returnObj = (*env)->AllocObject(env, objClass);
	if (returnObj == 0) printf("NULL RETURNED in AllocObject()\n");
	printf("Sizeof returnObj = %d\n", sizeof(returnObj) );
	************************************************** ************************/


	// Get the methodID for the constructor first
	constructorMethodID = env->GetMethodID(objCls, "<init>","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

	if (constructorMethodID == NULL) {
		LOGD("NULL RETURNED TO constructorMethodID\n");
		return NULL;
	}

	LOGD("constructorMethodID = %d\n",(int)constructorMethodID);

	rosterMap = native_client->GetRosterMap();

	if(rosterMap == NULL)
	{
		LOGD("Roster map is empty");
		return NULL;
	}

	LOG(INFO) <<  "Roster contains " <<  rosterMap->size() << "callable";

	// Create the object
	rosterSize = rosterMap->size(); //size of roster

	result = env->NewObjectArray(rosterSize, objCls, NULL);


	RosterMap::iterator iter = rosterMap->begin();

	i = 0;
	while (iter != rosterMap->end()) {

		LOG(INFO) <<  "JNICALL:Get roster map:" << iter->second.jid.BareJid().Str() << " -- " << DescribeStatus(iter->second.show, iter->second.status) ;

		const char* jid = iter->second.jid.BareJid().Str().c_str();
		const char* user = iter->second.jid.node().c_str();
		const char* status = DescribeStatus(iter->second.show, iter->second.status);

		returnObj = env->NewObject(objCls, constructorMethodID, env->NewStringUTF(jid), env->NewStringUTF(user), env->NewStringUTF(status));

		LOGD("Sizeof returnObj = %d\n", sizeof(returnObj) );

		env->SetObjectArrayElement(result, i, returnObj);

		env->DeleteLocalRef(returnObj);

		iter++;
		i++;
	}


	return result;
}


JNIEXPORT jobjectArray 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_getRoster
  (JNIEnv * env, jobject thiz) {

	jobjectArray result = NULL;

	result = InternalGetRoster(env, thiz);

	return result;

}

static jboolean InternalSendSubscriptionResponse
  (JNIEnv * env, jobject obj, jstring jid_str, jint type){
	const char *jid = env->GetStringUTFChars(jid_str,0);

	// type==3, subscribed; type==4 unsubscribed
	std::string s_jid(jid);

	LOGD("vtalk - sendSubscriptionResponse\n");

	native_client->SendSubscriptionMessage(s_jid, (int)type);

	env->ReleaseStringUTFChars(jid_str, jid);

	return JNI_TRUE;

}

JNIEXPORT jboolean 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendSubscriptionResponse
  (JNIEnv * env, jobject obj, jstring jid_str, jint type){
	jboolean b = JNI_TRUE;

	b = InternalSendSubscriptionResponse(env, obj, jid_str, type);

	return b;

}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_sendFile
  (JNIEnv * env, jobject thiz,
		  jstring id_str,
		  jstring jid_str,
		  jstring filepath_str,
		  jstring width_str,
		  jstring height_str,
		  jstring previewpath_str
		  )
{

//	LOGD("sendfile = \n");

//	const char *sid = env->GetStringUTFChars(id_str,0);
//	const char *jid = env->GetStringUTFChars(jid_str,0);
//	const char *filepath = env->GetStringUTFChars(filepath_str,0);
//	const char *w = env->GetStringUTFChars(width_str,0);
//	const char *h = env->GetStringUTFChars(height_str,0);
//	const char *previewpath = env->GetStringUTFChars(previewpath_str,0);
//
//	LOGD("sendfile = %s\n",jid);
//
//	std::string s_sessionId(sid);
//	std::string s_jid(jid);
//	std::string s_fpath(filepath);
//	std::string s_w(w);
//	std::string s_h(h);
//	std::string s_previewpath(previewpath);
//
//	native_client->SendFile(s_sessionId, s_jid, s_fpath, s_w, s_h, s_previewpath, "0");
//
//
//	env->ReleaseStringUTFChars(id_str, sid);
//	env->ReleaseStringUTFChars(jid_str, jid);
//	env->ReleaseStringUTFChars(filepath_str, filepath);
//	env->ReleaseStringUTFChars(width_str, w);
//	env->ReleaseStringUTFChars(height_str, h);
//	env->ReleaseStringUTFChars(previewpath_str, previewpath);

	LOGD("vtalk - sendFile\n");

	return (jint)0;
}

static jint InternalCancelFileShare(JNIEnv * env, jobject thiz, jstring filename_str)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalCancelFileShare - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *filename = env->GetStringUTFChars(filename_str,0);
		std::string s_filename(filename);

		LOGD("vtalk - cancel file share = %s\n",filename);

		native_client->CancelFileShare(s_filename);

		env->ReleaseStringUTFChars(filename_str, filename);
		return (jint)0;
	}
}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_cancelFileShare
(JNIEnv * env, jobject thiz, jstring filename_str){

	jint r = -1;

	r = InternalCancelFileShare(env, thiz, filename_str);

	return r;

}

static jint InternalAcceptFileShare
(JNIEnv * env, jobject thiz,
		  jstring share_id_str,
		  jstring samplerate_str,
		  jstring target_download_folder,
		  jstring target_download_file_name
		  )
{

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalAcceptFileShare - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *share_id = env->GetStringUTFChars(share_id_str,0);
	    const char *samplerate = env->GetStringUTFChars(samplerate_str,0);
	    const char *folder = env->GetStringUTFChars(target_download_folder,0);
	    const char *fname = env->GetStringUTFChars(target_download_file_name,0);


		std::string s_share_id(share_id);
	    std::string s_samplerate(samplerate);
	    std::string s_folder(folder);
	    std::string s_fname(fname);

	    LOGD("vtalk - accept file share id = %s, sample rate= %s, target folder=%s, target file name=%s\n",
	    		share_id,samplerate, folder, fname);

		native_client->AcceptFileShare(s_share_id, s_folder, s_fname);

		env->ReleaseStringUTFChars(share_id_str, share_id);
	    env->ReleaseStringUTFChars(samplerate_str, samplerate);
	    env->ReleaseStringUTFChars(target_download_folder, folder);
	    env->ReleaseStringUTFChars(target_download_file_name, fname);

		return (jint)0;
	}

}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_acceptFileShare
  (JNIEnv * env, jobject thiz,
		  jstring share_id_str,
		  jstring samplerate_str,
		  jstring target_download_folder,
		  jstring target_download_file_name
		  ){
	jint r = -1;

	r = InternalAcceptFileShare(env,
			thiz, share_id_str,
			samplerate_str, target_download_folder,
			target_download_file_name);

	return r;

}

static jint InternalDeclineFileShare
(JNIEnv * env, jobject thiz, jstring filename_str)
{
	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalDeclineFileShare - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *filename = env->GetStringUTFChars(filename_str,0);

		std::string s_filename(filename);

		LOGD("vtalk - decline file share = %s\n",filename);

		native_client->DeclineFileShare(s_filename);

		env->ReleaseStringUTFChars(filename_str, filename);
		return (jint)0;
	}


}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_declineFileShare
  (JNIEnv * env, jobject thiz, jstring filename_str) {
	jint r = -1;

	r = InternalDeclineFileShare(env,
			thiz, filename_str);

	return r;

}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_setFileShareFolder
(JNIEnv * env, jobject thiz, jstring path_str) {

	LOGD("vtalk - setFileShareFolder \n");

	return (jint)0;
}


static jint InternalDestroyClient  (JNIEnv * env, jobject obj)
{
	LOGD("vtalk - destroyClient \n");

	//TODO Maybe should delete native_client here.
	delete native_client;

	return (jint)0;
}


JNIEXPORT jint JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_destroyClient
  (JNIEnv * env, jobject obj) {

	jint r = -1;

	r = InternalDestroyClient(env, obj);

	return r;
}

//JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_stopStreamPlayer
//(JNIEnv * env, jobject thiz, jstring id_str){
//
//	const char *sid = env->GetStringUTFChars(id_str,0);
//
//	LOGD("vtalk - stopStreamPlayer = %s\n",sid);
//
//	std::string s_sessionId(sid);
//
//	native_client->StopStreamPlayer(s_sessionId);
//
//	env->ReleaseStringUTFChars(id_str, sid);
//	return (jint)0;
//
//}

//JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_resumeStreamPlayer
//(JNIEnv * env, jobject thiz, jstring id_str){
//
//	const char *sid = env->GetStringUTFChars(id_str,0);
//
//	LOGD("vtalk resumeStreamPlayer = %s\n",sid);
//
//	std::string s_sessionId(sid);
//
//	native_client->ResumeStreamPlayer(s_sessionId);
//
//	env->ReleaseStringUTFChars(id_str, sid);
//	return (jint)0;
//
//}

static jint InternalPlayMusic(JNIEnv * env, jobject thiz, jstring jid_str, jstring uuid_str)
{
	LOGD("vtalk - InternalPlayMusic\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalPlayMusic - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		const char *sid = env->GetStringUTFChars(uuid_str,0);
		const char *jid = env->GetStringUTFChars(jid_str,0);
		std::string s_jid(jid);

		LOGD("network player: play a song = %s\n",sid);

		std::string song_uuid(sid);

		int rt=native_client->Play(s_jid, song_uuid);

		env->ReleaseStringUTFChars(uuid_str, sid);
		env->ReleaseStringUTFChars(jid_str, jid);
		return (jint)rt;
	}


}



JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_play
(JNIEnv * env, jobject thiz, jstring jid_str, jstring uuid_str)
{
	jint r = -1;

	r = InternalPlayMusic(env, thiz, jid_str, uuid_str);

	return 0;

}

static jint InternalPauseMusic(JNIEnv * env, jobject thiz)
{
	LOGD("network player: InternalPauseMusic\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalPauseMusic - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
	    int rt=native_client->Pause();
		return (jint)rt;
	}

}


JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_pause
(JNIEnv * env, jobject thiz){

	jint r = -1;

	r = InternalPauseMusic(env, thiz);

	return r;
}

static jint InternalResumeMusic(JNIEnv * env, jobject thiz)
{
	LOGD("network player: InternalResumeMusic\n");

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalResumeMusic - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
	    int rt=native_client->Resume();
		return (jint)rt;
	}
}


JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_resume
(JNIEnv * env, jobject thiz){

	jint r = -1;

	r = InternalResumeMusic(env, thiz);

	return r;
}

static jint InternalSeek(JNIEnv * env, jobject thiz, jint position)
{
	LOGD("network player: InternalSeek: %d\n", position);

	if(native_client == NULL)
	{
		LOG(LS_ERROR) << "InternalSeek - Client instance is null, sth is wrong.";
		return -1;
	}
	else
	{
		int rt = native_client->Seek(position);
		return (jint)rt;
	}

}

JNIEXPORT jint 	JNICALL Java_com_vuece_vtalk_android_jni_JabberClient_seek
(JNIEnv * env, jobject thiz, jint position)
{
	jint r = -1;

	r = InternalSeek(env, thiz, position);

	return r;

}
