
#include "VueceNativeClientImplAndroid.h"

#include <cstdio>
#include <iostream>
#include <time.h>
#include <iomanip>
#include <cstdio>
#include <cstring>
#include <vector>
#include "talk/xmpp/constants.h"
#include "talk/base/logging.h"
#include "talk/base/flags.h"
#include "talk/base/stream.h"
#include "talk/base/thread.h"
#include "talk/base/physicalsocketserver.h"
#include "talk/xmpp/xmppclientsettings.h"

#include "talk/session/fileshare/VueceStreamPlayer.h"
#include "talk/session/fileshare/VueceNetworkPlayerFsm.h"

#ifdef ENABLE_PHONE_ENGINE
#include "talk/session/phone/VuecePhoneEngine.h"
#endif

#include "talk/base/ssladapter.h"

#include "xmppthread.h"
#include "xmppauth.h"
#include "VueceKernelShell.h"
#include "VueceGlobalSetting.h"
#include "VueceConstants.h"
#include "VueceLogger.h"

#include <android/log.h> //LogCat

using namespace vuece;

#ifdef ENABLE_PHONE_ENGINE
cricket::MediaEngine* CreateVuecePhoneEngine()
{
	cricket::VuecePhoneEngine* vPhoneEngine = new cricket::VuecePhoneEngine();
	return vPhoneEngine;
}
#endif

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////


static JavaVM *jvm;
static jobject iJNIListenerObj;
static jclass iJNIClassVTalkListener;
static jmethodID iJNIMethodOnPreviewReceived;
static jmethodID iJNIMethodOnClientStateChanged;
static jmethodID iJNIMethodOnMessage;
static jmethodID iJNIMethodOnSessionStateChanged;
static jmethodID iJNIMethodOnRosterStatusUpdate;
static jmethodID iJNIMethodOnReceiveVCard;
static jmethodID iJNIMethodOnRosterReceived;
static jmethodID iJNIMethodOnUnSubRespReceived;
static jmethodID iJNIMethodOnFileShareRequest;
static jmethodID iJNIMethodOnFileShareStateChanged;
static jmethodID iJNIMethodOnFileShareStateBytesUpdate;
static jmethodID iJNIMethodOnVhubGetMessageReceived;
static jmethodID iJNIMethodOnVhubResultMessageReceived;
static jmethodID iJNIMethodOnStreamPlayerStateChanged;

static jmethodID iJNIMethodOnNetworkPlayerStateChanged;
static jmethodID iJNIMethodOnMusicStreamingProgress;

static int iLoggingLevel;

static const int DEFAULT_PORT = 5222;

static bool onetime_init_done;
static 	VueceEvent exitReason;
static 	VueceCoreClient * client;

static void OneTimeInit(vuece::InitData* init_data);
static void InitLoggingFacility(int logging_level);

void OneTimeInit(vuece::InitData* init_data)
{
	LOG(INFO) << "VueceNativeClientImplAndroid():OneTimeInit - Start";
	jvm = (JavaVM*)init_data->p0;
	JNIEnv* env = (JNIEnv*)init_data->p1;
	jobject* aListener = (jobject*)init_data->p2;


	InitLoggingFacility( init_data->logging_level );

	client = NULL;

	exitReason = VueceEvent_Client_SignedOut;

	iJNIListenerObj = env->NewGlobalRef(*aListener);


	iJNIClassVTalkListener = (jclass) env->NewGlobalRef(env->GetObjectClass(*aListener));

	if (iJNIClassVTalkListener == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find java class VTalkListener!";
	}

	iJNIMethodOnClientStateChanged = env->GetMethodID(iJNIClassVTalkListener, "onClientStateChanged", "(II)V");

	if (iJNIMethodOnClientStateChanged == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onClientStateChanged";
	}
	LOG(INFO)
		<< "iJNIMethodOnClientStateChanged = " << iJNIMethodOnClientStateChanged;

	iJNIMethodOnNetworkPlayerStateChanged = env->GetMethodID(iJNIClassVTalkListener, "onNetworkPlayerStateChanged", "(II)V");

	if (iJNIMethodOnNetworkPlayerStateChanged == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onNetworkPlayerStateChanged";
	}

	LOG(INFO)
		<< "iJNIMethodOnNetworkPlayerStateChanged = " << iJNIMethodOnNetworkPlayerStateChanged;



	iJNIMethodOnMessage = env->GetMethodID(iJNIClassVTalkListener, "onMessage", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (iJNIMethodOnMessage == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onMessage";
	}

	iJNIMethodOnSessionStateChanged = env->GetMethodID(iJNIClassVTalkListener, "onSessionStateChanged", "(I)V");
	if (iJNIMethodOnSessionStateChanged == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onSessionStateChanged";
	}

	/*
	 * Java method signature:
	 *
	 * public void OnRosterStatusUpdate(
	 * final String jid,
	 * final int priority,
	 * final String status,
		final int show,
		final boolean hasPhone,
		final boolean hasVideo,
		final boolean hasCamera,
		final boolean hasMuc,
		final boolean hasShare,
		final boolean hasControl,
		final String iconHash,
		final String hubName,
		final String hubId)
		--------------------------------
		(Ljava/lang/String; 	jid
		I 						priority
		Ljava/lang/String; 		status
		I						show
		ZZZZZZ
		Ljava/lang/String;		iconHash
		Ljava/lang/String;		hubName
		Ljava/lang/String;)     hubId
		V
	 */
	iJNIMethodOnRosterStatusUpdate = env->GetMethodID(iJNIClassVTalkListener, "onRosterStatusUpdate", "(Ljava/lang/String;ILjava/lang/String;IZZZZZZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if (iJNIMethodOnRosterStatusUpdate == 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid:Cannot find method: VTalkListener:onStatusUpdate";
	}

	iJNIMethodOnReceiveVCard = env->GetMethodID(iJNIClassVTalkListener, "onReceiveVCard", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if (iJNIMethodOnReceiveVCard == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onReceiveVCard";
	}

	iJNIMethodOnRosterReceived = env->GetMethodID(iJNIClassVTalkListener, "onRosterReceived", "([Ljava/lang/Object;)V");
	if (iJNIMethodOnRosterReceived == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onRosterReceived";
	}

	iJNIMethodOnUnSubRespReceived = env->GetMethodID(iJNIClassVTalkListener, "onSubscriptionRequest", "(Ljava/lang/String;I)V");
	if (iJNIMethodOnUnSubRespReceived == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onSubscriptionRequest";
	}

	iJNIMethodOnFileShareStateChanged = env->GetMethodID(iJNIClassVTalkListener, "onFileShareStateChanged", "(Ljava/lang/String;I)V");

	if (iJNIMethodOnFileShareStateChanged == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onFileShareStateChanged";
	}
	LOG(INFO)
		<< "iJNIMethodOnFileShareStateChanged = " << iJNIMethodOnFileShareStateChanged;


	iJNIMethodOnFileShareStateBytesUpdate = env->GetMethodID(iJNIClassVTalkListener, "onFileShareStateBytesUpdate", "(Ljava/lang/String;I)V");

	if (iJNIMethodOnFileShareStateBytesUpdate == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onFileShareStateBytesUpdate";
	}
	LOG(INFO)
		<< "iJNIMethodOnFileShareStateBytesUpdate = " << iJNIMethodOnFileShareStateBytesUpdate;


	iJNIMethodOnFileShareRequest = env->GetMethodID(iJNIClassVTalkListener, "onFileShareRequest", "(Ljava/lang/Object;)V");

	if (iJNIMethodOnFileShareRequest == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onFileShareRequest";
	}
	LOG(INFO)
		<< "iJNIMethodOnFileShareStateBytesUpdate = " << iJNIMethodOnFileShareRequest;

	//  iJNIMethodOnVhubGetMessageReceived
	iJNIMethodOnVhubGetMessageReceived = env->GetMethodID(iJNIClassVTalkListener, "onVhubGetMessageReceived", "(Ljava/lang/String;Ljava/lang/String;)V");

	if (iJNIMethodOnVhubGetMessageReceived == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onVhubGetMessageReceived";
	}
	LOG(INFO)
		<< "iJNIMethodOnVhubGetMessageReceived = " << iJNIMethodOnVhubGetMessageReceived;

	// iJNIMethodOnVhubResultMessageReceived
	iJNIMethodOnVhubResultMessageReceived = env->GetMethodID(iJNIClassVTalkListener, "onVhubResultMessageReceived", "(Ljava/lang/String;Ljava/lang/String;)V");

	if (iJNIMethodOnVhubResultMessageReceived == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:onVhubResultMessageReceived";
	}
	LOG(INFO)
		<< "iJNIMethodOnVhubResultMessageReceived = " << iJNIMethodOnVhubResultMessageReceived;

	// iJNIMethodOnStreamPlayerStateChanged
	iJNIMethodOnStreamPlayerStateChanged = env->GetMethodID(iJNIClassVTalkListener, "onStreamPlayerStateChanged", "(I)V");

	if (iJNIMethodOnStreamPlayerStateChanged == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:iJNIMethodOnStreamPlayerStateChanged";
	}

	LOG(INFO)
		<< "iJNIMethodOnStreamPlayerStateChanged = " << iJNIMethodOnStreamPlayerStateChanged;

	// iJNIMethodOnPreviewReceived
	iJNIMethodOnPreviewReceived = env->GetMethodID(iJNIClassVTalkListener, "onPreviewReceived", "(Ljava/lang/String;)V");

	if (iJNIMethodOnPreviewReceived == 0) {
		LOG(LS_ERROR)
		<< "OneTimeInit:Cannot find method: VTalkListener:iJNIMethodOnPreviewReceived";
	}
	LOG(INFO)
	<< "iJNIMethodOnPreviewReceived = " << iJNIMethodOnPreviewReceived;

	// iJNIMethodOnMusicStreamingProgress
	iJNIMethodOnMusicStreamingProgress = env->GetMethodID(iJNIClassVTalkListener, "onMusicStreamingProgress", "(I)V");

	if (iJNIMethodOnMusicStreamingProgress == 0) {
		LOG(LS_ERROR)
			<< "OneTimeInit:Cannot find method: VTalkListener:iJNIMethodOnMusicStreamingProgress";
	}
	LOG(INFO)
		<< "iJNIMethodOnMusicStreamingProgress = " << iJNIMethodOnMusicStreamingProgress;

	if (env->ExceptionOccurred()) {
		env->ExceptionDescribe();
	}

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnClientStateChanged, (jint) CLIENT_EVENT_CLIENT_INITIATED, (jint) CLIENT_STATE_OFFLINE);

	VueceGlobalContext::Init(VueceAppRole_Media_Hub_Client);
	VueceGlobalContext::SetDeviceName((const char*)init_data->p3);
	VueceGlobalContext::SetAppVersion((const char*)init_data->p4);

	VueceStreamPlayer::SetJniEnv(env);

	LOG(INFO) << "VueceNativeClientImplAndroid():OneTimeInit - Done";
}

void InitLoggingFacility(int logging_level)
//void VueceNativeClientImplAndroid::InitLoggingFacility(int logging_level)
{

	VueceLogger::Debug("--------------------- InitLoggingFacility (%d) ------------------------", logging_level);

	//level definition in libjinle - logging.h
		/*
		 * enum LoggingSeverity
		 * {
		 * LS_SENSITIVE,
		 * LS_VERBOSE,
		 * LS_INFO,
		 * LS_WARNING,
		 * LS_ERROR,
		   INFO = LS_INFO,
		   WARNING = LS_WARNING,
		   LERROR = LS_ERROR };
		 */

		//TODO: Pass the value from java layer
		//format
		//[tstamp][thread][LOG-LEVEL][DEBUG_LEVEL][file]
		//tstamp: log timestamkp
		//thread: log thread number
		//LOG-LEVEL: set current log level
		//DEBUG-LEVEL: set the value of LOG-LEVEL to the value of DEBUG-LEVEL
		//			   that means only trace with log level higher than
		//file: set the value of file logging level to the value of LOG-LEVEL,
		//no file logging by default

	iLoggingLevel = logging_level;

	switch(iLoggingLevel)
	{
	case VUECE_LOG_LEVEL_DEBUG:

		VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level VUECE_LOG_LEVEL_DEBUG");

		talk_base::LogMessage::ConfigureLogging("tstamp thread verbose debug", "NOT_USED");
		break;
	case VUECE_LOG_LEVEL_INFO:

		VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level VUECE_LOG_LEVEL_DEBUG");

		talk_base::LogMessage::ConfigureLogging("tstamp thread info debug", "NOT_USED");
		break;
	case VUECE_LOG_LEVEL_WARN:

		VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level VUECE_LOG_LEVEL_DEBUG");

		talk_base::LogMessage::ConfigureLogging("tstamp thread warning debug", "NOT_USED");
		break;
	case VUECE_LOG_LEVEL_ERROR:

		VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level VUECE_LOG_LEVEL_DEBUG");

		talk_base::LogMessage::ConfigureLogging("tstamp thread error debug", "NOT_USED");
		break;
	case VUECE_LOG_LEVEL_NONE:

		VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level VUECE_LOG_LEVEL_DEBUG");

		talk_base::LogMessage::ConfigureLogging("none", "NOT_USED");
		break;
	default:

		VueceLogger::Fatal("VueceNativeClientImplAndroid - configure logging level not handled");

		break;
	}

	VueceLogger::Debug("VueceNativeClientImplAndroid - configure logging level done");

}

VueceNativeClientImplAndroid::VueceNativeClientImplAndroid(vuece::InitData* init_data)
{
#ifdef VUECE_APP_ROLE_HUB_CLIENT
	LOG(INFO) << "VueceNativeClientImplAndroid::Constructor - This is a hub client";
#endif

	if(!onetime_init_done)
	{
		LOG(INFO) << "VueceNativeClientImplAndroid():Constructor called - Performing one-time init";

		OneTimeInit(init_data);

		onetime_init_done = true;
	}
	else
	{
		LOG(INFO) << "VueceNativeClientImplAndroid():Constructor called - One-time init already done.";
	}

 	LOG(INFO) << "VueceNativeClientImplAndroid():Constructor Done.";

}

VueceNativeClientImplAndroid::~VueceNativeClientImplAndroid() {

	LOG(INFO)
		<< "VueceNativeClientImplAndroid - Destructor called.";

	//todo - put this somewhere else
//	JNIEnv *env = 0;
//	jvm->AttachCurrentThread(&env, NULL);
//	env->DeleteGlobalRef(iJNIListenerObj);
//	env->DeleteGlobalRef(iJNIClassVTalkListener);

	//won't work
//	jvm->DetachCurrentThread();


	LOG(INFO)
		<< "VueceNativeClientImplAndroid - De-constructor Done.";

	//NOTE: DONT TRY TO DELETE ANY THING HERE!!!!

}

RosterMap* VueceNativeClientImplAndroid::GetRosterMap() {
	if (client == NULL) {
		return NULL;
	}

	return client->GetRosterMap();
}

int VueceNativeClientImplAndroid::SendVHubMessage(const std::string& to,const std::string& type,const std::string& message) {
	LOG(INFO) << "VueceNativeClientImplAndroid - SendVHubMessage";

	if (client == NULL) {

		LOG(INFO) << "VueceNativeClientImplAndroid - SendVHubMessage: client is null, do nothing";
		return RESULT_FUNC_NOT_ALLOWED;
	}

	client->SendVHubMessage(to,type,message);

	return RESULT_OK;

}

int VueceNativeClientImplAndroid::SendVHubPlayRequest(const std::string& to, const std::string& type,const std::string& message, const std::string& uri)
{
	LOG(INFO) << "VueceNativeClientImplAndroid - SendVHubMessage";

	if (client == NULL) {

		LOG(INFO) << "VueceNativeClientImplAndroid - SendVHubMessage: client is null, do nothing";
		return RESULT_FUNC_NOT_ALLOWED;
	}

	client->SendVHubPlayRequest(to,type,message, uri);
}


void VueceNativeClientImplAndroid::AddBuddy(const std::string& jid) {
	if (client == NULL) {
		return;
	}

	client->InviteFriend(jid);

}

void VueceNativeClientImplAndroid::SendFile(
		const std::string& share_id,
		const std::string& jid,
		const std::string& pathname,
		const std::string& width,
		const std::string& height,
		const std::string& preview_file_path,
		const std::string& start_pos,
		const std::string& need_preview
		)
{

	LOG(INFO)
		<< "VueceNativeClientImplAndroid::SendFile: (" << share_id << ")" << jid << " - " << pathname << ", start position: " << start_pos;

	int i;
	char buf[512];

	//here we use ascii code FS to separate param1 and param2 and param3 in one string
	memset(buf, '\0', sizeof(buf));
	strcpy(buf, share_id.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, jid.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, pathname.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, width.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, height.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, preview_file_path.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, start_pos.c_str());

	i = strlen(buf);
	buf[i] = VUECE_FOLDER_SEPARATOR;
	strcat(buf, need_preview.c_str());

	SigVueuceCommandMessage(VUECE_CMD_SEND_FILE, buf);
}


void VueceNativeClientImplAndroid::CancelFileShare(const std::string& share_id){
	LOG(INFO)
		<< "VueceNativeClientImplAndroid::CancelFileShare: " << share_id;
	SigVueuceCommandMessage(VUECE_CMD_CANCEL_FILE, share_id.c_str());
}

void VueceNativeClientImplAndroid::AcceptFileShare(
		const std::string& share_id,
		const std::string& target_folder,
		const std::string& target_file_name)
{

	LOG(INFO) << "VueceNativeClientImplAndroid::AcceptFile: " << share_id
			<< ", target_folder: " << target_folder
			<< ", target_file_name: " << target_file_name;

	int i;
	char buf[128];

	//here we use ascii code FS to separate param1 and param2 and param3 in one string
	memset(buf, '\0', sizeof(buf));

	//share_id
	strcpy(buf, share_id.c_str());

	//target_folder
	i = strlen(buf);
	buf[i] = 0x1C;
	strcat(buf, target_folder.c_str());

	//target_file_name
	i = strlen(buf);
	buf[i] = 0x1C;
	strcat(buf, target_file_name.c_str());

	SigVueuceCommandMessage(VUECE_CMD_ACCEPT_FILE, buf);
}

void VueceNativeClientImplAndroid::DeclineFileShare(const std::string& share_id){
	LOG(INFO)
		<< "VueceNativeClientImplAndroid::DeclineFileShare: " << share_id;
	SigVueuceCommandMessage(VUECE_CMD_DECLINE_FILE, share_id.c_str());
}

void VueceNativeClientImplAndroid::SendSubscriptionMessage(const std::string& jid, int type) {
	if (client == NULL) {
		return;
	}

	client->SendSubscriptionMessage(jid, type);
}

void VueceNativeClientImplAndroid::SendPresence(const std::string& status)
{
	if (client == NULL) {
		return;
	}

	client->SendPresence(status);
}

void VueceNativeClientImplAndroid::SendPresence(const std::string& status, const std::string& signature) {
	if (client == NULL) {
		return;
	}

	client->SendPresence(status, signature);
}

void VueceNativeClientImplAndroid::SendSignature(const std::string& sig) {
	if (client == NULL) {
		return;
	}

	client->SendSignature(sig);
}

int VueceNativeClientImplAndroid::Start(const char* name, const char* pwd, const int auth_type) {

	LOG(INFO) << "VueceNativeClientImplAndroid::Start - Start";

	if(client != NULL)
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::Start - VueceCoreClient instance should be null, abort now!");
		return RESULT_FUNC_NOT_ALLOWED;
	}

	client = new VueceCoreClient(this, name);

    SigVueuceCommandMessage.connect(client, &VueceCoreClient::OnVueceCommandReceived);

	//a blocking call
    //TODO - Use VueceConsole to wrap core client
	client->Start(name, pwd,  auth_type);

	LOG(INFO) << "VueceNativeClientImplAndroid - VueceCoreClient::Start returned, deleting the instance";

	delete client;

	client = NULL;

	LOG(INFO) << "====================================================";
	LOG(INFO) << "VueceNativeClientImplAndroid::Start:Call client finalized!!!!!";
	LOG(INFO) << "====================================================";

	return RESULT_OK;
}

cricket::CallOptions* VueceNativeClientImplAndroid::GetCurrentCallOption(void)
{
	return client->GetCurrentCallOption();
}

buzz::Jid VueceNativeClientImplAndroid::GetCurrentJidInCall(void)
{
	return client->GetCurrentTargetJid();
}


int VueceNativeClientImplAndroid::LogOut()
{
	LOG(INFO) << "VueceNativeClientImplAndroid::LogOut";

	return client->LogOut();
}


#ifdef ENABLE_PHONE_ENGINE
void VueceNativeClientImplAndroid::HangUpCall() {
	//send a hangup signal
	LOG(INFO) << "HangUpCall";
	SigVueuceCommandMessage(VUECE_CMD_HANG_UP, NULL);
}

void VueceNativeClientImplAndroid::PlaceVoiceCall(const char* target, const char* sid) {
	LOG(INFO) << "VueceNativeClientImplAndroid::PlaceVoiceCall:Target: " << target;

	int i;
	char buf[256];

	//here we use ASCII code FS to separate param1 and param2 in one string
	memset(buf, '\0', sizeof(buf));
	strcpy(buf, param1);

	i = strlen(buf);

	buf[i] = VUECE_FOLDER_SEPARATOR;

	strcat(buf, param2);

	SigVueuceCommandMessage(VUECE_CMD_PLACE_VOICE_CALL_TO_REMOTE_TARGET, buf);
}

void VueceNativeClientImplAndroid::PlaceVideoCall(const char* target, const char* sid) {
	LOG(INFO)<< "VueceNativeClientImplAndroid::PlaceVideoCall:Target: " << target;

	int i;
	char buf[256];

	//here we use ASCII code FS to separate param1 and param2 in one string
	memset(buf, '\0', sizeof(buf));
	strcpy(buf, param1);

	i = strlen(buf);

	buf[i] = VUECE_FOLDER_SEPARATOR;

	strcat(buf, param2);

	SigVueuceCommandMessage(VUECE_CMD_PLACE_VIDEO_CALL_TO_REMOTE_TARGET, buf);
}


void VueceNativeClientImplAndroid::RejectCall() {
	LOG(INFO) << "VueceNativeClientImplAndroid::RejectCall";
	SigVueuceCommandMessage(VUECE_CMD_REJECT_CALL, NULL);
}

void VueceNativeClientImplAndroid::AcceptCall() {
	LOG(INFO) << "VueceNativeClientImplAndroid::AcceptCall";
	SigVueuceCommandMessage(VUECE_CMD_ACCEPT_CALL, NULL);
}
#endif

void VueceNativeClientImplAndroid::OnCallSessionState(const int state) {
	LOG(INFO) << "VueceNativeClientImplAndroid::OnCallSessionState";
	JNIEnv *env = 0;
	jint result = jvm->AttachCurrentThread(&env, NULL);
	if (result != 0) {
		LOG(LS_ERROR) << "VueceNativeClientImplAndroid::OnCallSessionState:Cannot attach JVM!";
		return;
	}

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnSessionStateChanged, (jint) state);

}

void VueceNativeClientImplAndroid::OnRosterStatusUpdate(const buzz::Status& status) {

	LOG(INFO) << "VueceNativeClientImplAndroid: OnRosterStatusUpdate";

	JNIEnv *env = 0;

	jint result = jvm->AttachCurrentThread(&env, NULL);

	if (result != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnRosterStatusUpdate:Cannot attach JVM!";
		return;
	}

	std::string jid_std_s (status.jid().Str());
	std::string status_std_s (status.status());
	std::string icon_std_s (status.photo_hash());
	std::string hub_name_std_s (status.hub_name());
	std::string hub_id_std_s (status.hub_id());

	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:jid_std_s:[" << jid_std_s << "]";

	const char* jid = jid_std_s.c_str();
	const char* status_char = status_std_s.c_str();
	const char* icon = icon_std_s.c_str();
    const char* hub_name = hub_name_std_s.c_str();
    const char* hub_id = hub_id_std_s.c_str();

	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:Jid:[" << jid << "]";
	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:Status:[" << status_char << "]";
	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:HubName:[" << hub_name << "]";
	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:HubId:[" << hub_id << "]";

	jstring jid_s = env->NewStringUTF(jid);
	jstring status_s = env->NewStringUTF(status_char);
	jstring icon_s = env->NewStringUTF(icon);
    jstring hub_name_s = env->NewStringUTF(hub_name);
    jstring hub_id_s = env->NewStringUTF(hub_id);

	env->CallVoidMethod(iJNIListenerObj,
			iJNIMethodOnRosterStatusUpdate,
			jid_s,
			(jint) status.priority(),
			status_s,
			(jint) status.show(),
			(jboolean) status.phone_capability(),
			(jboolean) status.video_capability(),
			(jboolean) status.camera_capability(),
			(jboolean) status.pmuc_capability(),
			(jboolean) status.fileshare_capability(),
			(jboolean) status.vhub_capability(),
			icon_s,
			hub_name_s,
            hub_id_s);

	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:Jid 2 :[" << jid << "]";
	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:Status 2 :[" << status_char << "]";
	LOG(INFO) << "VueceNativeClientImplAndroid:OnRosterStatusUpdate:Hub 2 :[" << hub_name << "]";

	env->DeleteLocalRef(jid_s);
	env->DeleteLocalRef(status_s);
	env->DeleteLocalRef(icon_s);
    env->DeleteLocalRef(hub_name_s);
    env->DeleteLocalRef(hub_id_s);

}

#ifdef CHAT_ENABLED
void VueceNativeClientImplAndroid::SendChat(const std::string& to, const std::string& msg) {
	if (client == NULL) {
		return;
	}

	client->SendChat(to, msg);
}

void VueceNativeClientImplAndroid::OnChatMessageReceived(const buzz::Jid& from, const std::string& msg) {
	LOG(LS_VERBOSE)
		<< "OnChatMessageReceived:Received a msg from: " << from.node();
	LOG(LS_VERBOSE)
		<< "OnChatMessageReceived:Message content:\n-------------------\n" << msg << "\n-------------------\n";

	JNIEnv *env = 0;

	jint result = jvm->AttachCurrentThread(&env, NULL);

	if (result != 0) {
		LOG(LS_ERROR) << "VueceNativeClientImplAndroid::OnChatMessageReceived:Cannot attach JVM!";
		return;
	}

	std::string jid_std_s (from.Str());
	std::string msg_std_s (msg);

	const char* jid = jid_std_s.c_str();
	const char* msgText = msg_std_s.c_str();

	jstring jid_s = env->NewStringUTF(jid);
	jstring msgText_s = env->NewStringUTF(msgText);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnMessage, jid_s, msgText_s);

	env->DeleteLocalRef(jid_s);
	env->DeleteLocalRef(msgText_s);

	LOG(LS_VERBOSE) << "OnChatMessageReceived:Chat message delivered.";

}
#endif

#ifdef VCARD_ENABLED
void VueceNativeClientImplAndroid::SendVCardRequest(const std::string& to) {
	if (client == NULL) {
		return;
	}

	client->SendVCardRequest(to);

}
void VueceNativeClientImplAndroid::OnRosterVCardReceived(const std::string& jid, const std::string& fn, const std::string& imgData) {
	LOG(LS_VERBOSE)
		<< "OnRosterVCardReceived:Received a vcard query result for: " << jid;
	LOG(LS_VERBOSE)
		<< "OnRosterVCardReceived:Full name:" << fn;

	JNIEnv *env = 0;

	jint result = jvm->AttachCurrentThread(&env, NULL);

	if (result != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnRosterVCardReceived:Cannot attach JVM!";
		return;
	}

	std::string jid_std_s (jid);
	std::string fn_std_s (fn);
	std::string photo_std_s (imgData);

	jstring jid_s = env->NewStringUTF(jid_std_s.c_str());
	jstring fn_s = env->NewStringUTF(fn_std_s.c_str());
	jstring photo_s = env->NewStringUTF(photo_std_s.c_str());

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnReceiveVCard, jid_s, fn_s, photo_s);

	env->DeleteLocalRef(jid_s);
	env->DeleteLocalRef(fn_s);
	env->DeleteLocalRef(photo_s);

	LOG(LS_VERBOSE) << "OnRosterVCardReceived:vCard message delivered.";
}
#endif

void VueceNativeClientImplAndroid::OnRosterSubRespReceived(VueceRosterSubscriptionMsg* m) {

	LOG(LS_VERBOSE) << "OnRosterSubRespReceived";
//	VueceLogger::Warn("OnRosterSubRespReceived");
	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	int type = m->subscribe_type;

	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnRosterSubRespReceived:Cannot attach JVM!";
		return;
	}

//	std::string messageType = stanza->Attr(buzz::QN_TYPE);
//	std::string from = stanza->Attr(buzz::QN_FROM);
//	std::string messageType = stanza->Attr(buzz::QN_TYPE);
	std::string from (m->user_id);

	LOG(LS_VERBOSE) << "OnRosterSubRespReceive::Source: " << from << ", type: " << type;
//	VueceLogger::Warn("OnRosterSubRespReceive::Source: %s, type: %s", from.c_str(), messageType.c_str());

//	if (messageType == buzz::STR_SUBSCRIBED) {
//		LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: subscribed";
//		type = SUBSCRIPTION_TYPE_SUBSCRIBED;
//	} else 	if (messageType == buzz::STR_SUBSCRIBE) {
//		LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: subscribe";
//		type = SUBSCRIPTION_TYPE_SUBSCRIBE;
//	} else 	if (messageType == buzz::STR_UNSUBSCRIBE) {
//		LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: unsubscribe";
//		type = SUBSCRIPTION_TYPE_UNSUBSCRIBE;
//	} else 	if (messageType == buzz::STR_UNSUBSCRIBED) {
//		LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: unsubscribed";
//		type = SUBSCRIPTION_TYPE_UNSUBSCRIBED;
//	} else 	if (messageType == buzz::STR_UNAVAILABLE) {

	if(type == SUBSCRIPTION_TYPE_UNAVAILABLE){
		LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: unavailable";
//		type = SUBSCRIPTION_TYPE_UNAVAILABLE;

		//check if this is from user's own hub, if yes, stop current play if needed.
		if(strcmp(from.c_str(), VueceGlobalContext::GetCurrentServerNodeJid()) == 0)
		{
			LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Message type: Ower's hub is not available, stop playing now";

			//Pause(); Not working, because called in the same thread.
			VueceGlobalContext::SetCurrentTransactionCancelled(true);
			VueceStreamPlayer::StopAndResetPlayerAsync();
			client->OnCurrentRemoteHubUnAvailable();
		}
	}

	const char* jid = from.c_str();
	jstring jid_s = env->NewStringUTF(jid);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnUnSubRespReceived, jid_s, (jint)type);

	env->DeleteLocalRef(jid_s);

	LOG(LS_VERBOSE) << "OnRosterSubRespReceived:Notification delivered to upper layer";
}

void VueceNativeClientImplAndroid::OnRosterReceived(const buzz::XmlElement* stanza) {
	LOG(INFO) << "OnRosterReceived";
//	VueceLogger::Warn("OnRosterReceived. %s", (stanza->Str()).c_str());
	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnRosterReceived:Cannot attach JVM!";
		return;
	}

	std::vector < std::string > names;

	jmethodID constructorMethodID;
	jobject rosterObj;
	jobjectArray rosterObjArray;
	jclass objCls = env->FindClass("com/vuece/vtalk/android/model/RosterItem");
	int rosterSize = 0;
	int i = 0;

	if (objCls == NULL) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnRosterReceived:cannot find RosterItem class";
		return;
	}

	constructorMethodID = env->GetMethodID(objCls, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnRosterReceived:Processing roster iq result...";

	const buzz::XmlElement * query = stanza->FirstNamed(buzz::QN_ROSTER_QUERY);
//	VueceLogger::Warn("query. %s", (query->Str()).c_str());
	//count size at first
	const buzz::XmlElement* rosterItem = query->FirstNamed(buzz::QN_ROSTER_ITEM);
	while (rosterItem) {
//		VueceLogger::Warn("rosterItem. %s", (rosterItem->Str()).c_str());
		rosterSize++;
		rosterItem = rosterItem->NextNamed(buzz::QN_ROSTER_ITEM);
	}

	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnRosterReceived:Roster size = " << rosterSize;
	VueceLogger::Warn("VueceNativeClientImplAndroid:OnRosterReceived:Roster size = %i", rosterSize);
	//create roster array object
	rosterObjArray = env->NewObjectArray(rosterSize, objCls, NULL);

	for (const buzz::XmlElement* rosterItem = query->FirstNamed(buzz::QN_ROSTER_ITEM); rosterItem; rosterItem = rosterItem->NextNamed(buzz::QN_ROSTER_ITEM)) {

		std::string jid = cricket::GetXmlAttr(rosterItem, buzz::QN_JID, buzz::STR_EMPTY);
		std::string sub = cricket::GetXmlAttr(rosterItem, buzz::QN_SUBSCRIPTION, buzz::STR_EMPTY);
		std::string name = cricket::GetXmlAttr(rosterItem, buzz::QN_NAME, buzz::STR_EMPTY);

		LOG(LS_VERBOSE)
			<< "VueceNativeClientImplAndroid:OnRosterReceived:Roster item:jid = " << jid << ", subscription = " << sub << ", name: " << name;
//		VueceLogger::Warn("VueceNativeClientImplAndroid:OnRosterReceived:Roster item = %s", jid.c_str());
		const char* jid_str = jid.c_str();
		const char* sub_str = sub.c_str();
		const char* name_str = name.c_str();

		jstring jidObj = env->NewStringUTF(jid_str);
		jstring subObj = env->NewStringUTF(sub_str);
		jstring nameObj = env->NewStringUTF(name_str);
		rosterObj = env->NewObject(objCls, constructorMethodID, jidObj, subObj, nameObj);

		env->SetObjectArrayElement(rosterObjArray, i, rosterObj);

		env->DeleteLocalRef(rosterObj);
		env->DeleteLocalRef(jidObj);
		env->DeleteLocalRef(subObj);
		env->DeleteLocalRef(nameObj);

		i++;

	}

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnRosterReceived, rosterObjArray);

	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnRosterReceived:Roster list delivered to upper layer.";

	env->DeleteLocalRef(rosterObjArray);

}

void VueceNativeClientImplAndroid::OnFileSharePreviewReceived(const std::string& share_id, const std::string& path, int w, int h)
{
	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnFileSharePreviewReceived:share_id: " << share_id << ", path: " << path << ", w: " << w << ", h: " << h;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnFileSharePreviewReceived:Cannot attach JVM!";
		return;
	}

	std::string share_id_std_s (share_id);
	std::string path_std_s (path);

    const char* uid_str = share_id_std_s.c_str();
	jstring uid_s = env->NewStringUTF(uid_str);

    const char* path_str = path_std_s.c_str();
	jstring path_s = env->NewStringUTF(path_str);
    //iJNIMethodOnPreviewReceived = (jmethodID)0x4c280808;
    LOG(LS_VERBOSE) << "before calling java method" << iJNIMethodOnPreviewReceived;
	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnPreviewReceived, path_s);
	env->DeleteLocalRef(uid_s);
	env->DeleteLocalRef(path_s);

}

vuece::NetworkPlayerState VueceNativeClientImplAndroid::GetNetworkPlayerState(void)
{
	vuece::NetworkPlayerState ret = vuece::NetworkPlayerState_None;

	ret = VueceNetworkPlayerFsm::GetNetworkPlayerState();

	return ret;
}

void VueceNativeClientImplAndroid::OnNetworkPlayerStateChanged(vuece::NetworkPlayerEvent e, vuece::NetworkPlayerState s)
{
	char buf1[32+1];
	char buf2[32+1];

	VueceNetworkPlayerFsm::GetNetworkPlayerEventString(e, buf1);
	VueceNetworkPlayerFsm::GetNetworkPlayerStateString(s, buf2);

	VueceLogger::Debug("VueceNativeClientImplAndroid::OnNetworkPlayerStateChanged(), event: %s, state: %s", buf1, buf2);

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnNetworkPlayerStateChanged:Cannot attach JVM!";
		return;
	}
	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnNetworkPlayerStateChanged, (jint) e, (jint) s);

}

bool VueceNativeClientImplAndroid::IsMusicStreaming(void)
{
	if(client != NULL)
	{
		return client->IsMusicStreaming();
	}
	else
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::IsMusicStreaming() - This is method is called but core client is null, abort now!");
		return false;
	}
}

int VueceNativeClientImplAndroid::GetCurrentPlayingProgress(void)
{
	if(client != NULL)
	{
		return client->GetCurrentPlayingProgress();
	}
	else
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::GetCurrentPlayingProgress() - This is method is called but core client is null, abort now!");
		return -1;
	}

	return -1;
}


void VueceNativeClientImplAndroid::OnStreamPlayerStateChanged(const std::string& share_id, int state)
{
	LOG(LS_VERBOSE) << "VueceNativeClientImplAndroid::OnStreamPlayerStateChanged:share_id: " << share_id << ", state: " << state;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnStreamPlayerStateChanged:Cannot attach JVM!";
		return;
	}

	std::string share_id_std_s (share_id);

	const char* share_id_str = share_id_std_s.c_str();
	jstring share_id_s = env->NewStringUTF(share_id_str);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnStreamPlayerStateChanged, (jint) state);
	env->DeleteLocalRef(share_id_s);

}

void VueceNativeClientImplAndroid::OnFileShareProgressUpdated(const std::string& share_id, int percent)
{
	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnFileShareProgressUpdated:share_id: " << share_id << ", progress(%): " << percent;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnFileShareProgressUpdated:Cannot attach JVM!";
		return;
	}

	std::string share_id_std_s (share_id);

	const char* share_id_str = share_id_std_s.c_str();
	jstring share_id_s = env->NewStringUTF(share_id_str);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnFileShareStateBytesUpdate, share_id_s, (jint) percent);
	env->DeleteLocalRef(share_id_s);
}

void VueceNativeClientImplAndroid::OnMusicStreamingProgressUpdated(const std::string& share_id, int progress)
{
//	LOG(LS_VERBOSE)
//		<< "VueceNativeClientImplAndroid:OnMusicStreamingProgressUpdated:share_id: "
//		<< share_id << ", progress(second): " << progress;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnMusicStreamingProgressUpdated:Cannot attach JVM!";
		return;
	}

	std::string share_id_std_s (share_id);

	const char* share_id_str = share_id_std_s.c_str();
	jstring share_id_s = env->NewStringUTF(share_id_str);

//	LOG(LS_VERBOSE) << "VueceNativeClientImplAndroid:OnMusicStreamingProgressUpdated - Calling iJNIMethodOnMusicStreamingProgress";

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnMusicStreamingProgress, (jint) progress);
	env->DeleteLocalRef(share_id_s);
}


//TODO Finish your work here
int VueceNativeClientImplAndroid::GetCurrentMusicStreamingProgress(const std::string& share_id)
{
	VueceLogger::Debug("VueceNativeClientImplAndroid::GetCurrentMusicStreamingProgress");

	if(client == NULL)
	{
		//VueceLogger::Fatal("VueceNativeClientImplAndroid::GetCurrentMusicStreamingProgress() - Called when client is NULL, abort now.");
		return 0;
	}
	else
	{
		return client->GetCurrentMusicStreamingProgress(share_id);
	}
}


void VueceNativeClientImplAndroid::OnFileShareStateChanged(const std::string& remote_jid, const std::string& share_id, int state)
{
	LOG(LS_INFO)
		<< "VueceNativeClientImplAndroid::OnFileShareStateChanged:remote_jid: " << remote_jid << ", share_id: " << share_id << ", state: " << state
		<< ", empty impl, do nothing and return.";
}

void VueceNativeClientImplAndroid::OnFileShareRequestReceived(const std::string& share_id, const buzz::Jid& target, int type, const std::string& fileName, int size, bool need_preview)
{
	LOG(LS_INFO)
		<< "VueceNativeClientImplAndroid::OnFileShareRequestReceived:File: " << fileName << ", size: " << size;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnFileShareRequestReceived:Cannot attach JVM!";
		return;
	}

	jmethodID constructorMethodID;
	jobject fsOptObj;
    std::string jid_std_s (target.Str());
	jclass objCls = env->FindClass("com/vuece/vtalk/android/model/FileShareOption");
	if (objCls == NULL) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnFileShareRequestReceived:cannot find FileShareOption class";
		return;
	}

	constructorMethodID = env->GetMethodID(objCls, "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;IZ)V");

	const char* share_id_str = share_id.c_str();
	const char* jid_str = jid_std_s.c_str();
	const char* f_str = fileName.c_str();

	jstring share_id_obj = env->NewStringUTF(share_id_str);
	jstring jidObj = env->NewStringUTF(jid_str);
	jstring fObj = env->NewStringUTF(f_str);

	fsOptObj = env->NewObject(objCls, constructorMethodID, share_id_obj, jidObj, (jint)type, fObj, (jint)size, (jboolean)need_preview);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnFileShareRequest, fsOptObj);

	LOG(LS_VERBOSE)
		<< "VueceNativeClientImplAndroid:OnFileShareRequestReceived:File share option object delivered to upper layer.";

	env->DeleteLocalRef(fsOptObj);
	env->DeleteLocalRef(share_id_obj);
	env->DeleteLocalRef(jidObj);
	env->DeleteLocalRef(fObj);

}

void VueceNativeClientImplAndroid::OnVHubGetMessageReceived(const buzz::Jid& jid, const std::string& message)
{
	LOG(LS_INFO)
		<< "VueceNativeClientImplAndroid::OnVHubGetMessageReceived:jid: " << jid.Str().c_str() << ", message: " << message;
	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnVHubGetMessageReceived:Cannot attach JVM!";
		return;
	}

	std::string jid_std_s(jid.Str());

	const char* jid_str = jid_std_s.c_str();
	jstring jid_s = env->NewStringUTF(jid_str);

	jstring msg_s = env->NewStringUTF(message.c_str());

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnVhubGetMessageReceived, jid_s, msg_s);

	env->DeleteLocalRef(jid_s);
	env->DeleteLocalRef(msg_s);

}

void VueceNativeClientImplAndroid::OnVHubResultMessageReceived(const buzz::Jid& jid, const std::string& message)
{
	LOG(LS_INFO)
		<< "VueceNativeClientImplAndroid::OnVHubResultMessageReceived:jid: " << jid.Str().c_str() << ", message: " << message;

	JNIEnv *env = 0;
	jint ret = jvm->AttachCurrentThread(&env, NULL);
	if (ret != 0) {
		LOG(LS_ERROR)
			<< "VueceNativeClientImplAndroid::OnVHubResultMessageReceived:Cannot attach JVM!";
		return;
	}

	std::string jid_std_s (jid.Str());

	const char* jid_str = jid_std_s.c_str();
	jstring jid_s = env->NewStringUTF(jid_str);

	jstring msg_s = env->NewStringUTF(message.c_str());

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnVhubResultMessageReceived, jid_s, msg_s);

	env->DeleteLocalRef(jid_s);
	env->DeleteLocalRef(msg_s);

}

vuece::ClientState VueceNativeClientImplAndroid::GetClientState(void)
{
	if(client != NULL)
	{
		return client->GetClientState();
	}
	else
	{
		VueceLogger::Warn("VueceNativeClientImplAndroid::GetClientState - VueceCoreClient is null, return OFFLINE by default");
		return CLIENT_STATE_OFFLINE;
	}

}

void VueceNativeClientImplAndroid::OnClientStateChanged(vuece::ClientEvent event, vuece::ClientState state)
{

	JNIEnv *env = 0;
	jint result = jvm->AttachCurrentThread(&env, NULL);
	if (result != 0) {
		LOG(LS_ERROR) << "VueceNativeClientImplAndroid::OnClientStateChanged:Cannot attach JVM!";
		return;
	}


	LOG(INFO) << "VueceNativeClientImplAndroid::OnClientStateChanged - event = " << event << ", state = " << state ;

	VueceNativeInterface::LogClientEvent(event);
	VueceNativeInterface::LogClientState(state);

	env->CallVoidMethod(iJNIListenerObj, iJNIMethodOnClientStateChanged, (jint) event, (jint) state);
}

void VueceNativeClientImplAndroid::TestSendFile()
{

}

//file system
void VueceNativeClientImplAndroid::OnFileSystemChanged(void)
{

}

void VueceNativeClientImplAndroid::OnRemoteDeviceActivity(VueceStreamingDevice* d)
{

}

int VueceNativeClientImplAndroid::Play(const std::string &jid, const std::string& song_uuid)
{
	int ret = vuece::RESULT_FUNC_NOT_ALLOWED;

	VueceLogger::Debug("VueceNativeClientImplAndroid::Play - Start");

	if (client->GetShell() != NULL)
	{
		ret = client->GetShell()->PlayMusic(jid, song_uuid);
	}
	else
	{
		ret = vuece::RESULT_GENERAL_ERR;

		VueceLogger::Fatal("VueceNativeClientImplAndroid::Play - Core shell is null, abort now.");
	}

	VueceLogger::Debug("VueceNativeClientImplAndroid::Play - End with return code: %d", ret);

	return ret;
}

int VueceNativeClientImplAndroid::Pause()
{
	int ret = vuece::RESULT_GENERAL_ERR;

	VueceLogger::Debug("VueceNativeClientImplAndroid::Pause");

	if (client->GetShell() != NULL)
	{
		VueceLogger::Debug("VueceNativeClientImplAndroid::pass true to check network player state at first");

		//pass true to check network player state at first
		ret = client->GetShell()->PauseMusic(true);

		VueceLogger::Debug("VueceNativeClientImplAndroid::Pause() returned with code: %d", ret);
	}
	else
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::Pause - Core shell is null, abort now.");

		ret = vuece::RESULT_FUNC_NOT_ALLOWED;

	}

	VueceLogger::Debug("VueceNativeClientImplAndroid::Pause Done, returning code: %d", ret);

	return ret;
}

int VueceNativeClientImplAndroid::Resume()
{
	VueceLogger::Debug("VueceNativeClientImplAndroid::Resume");

	if (client->GetShell() != NULL)
	{
		return client->GetShell()->ResumeMusic();
	}
	else
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::Resume - Core shell is null, abort now.");
	}

	VueceLogger::Debug("VueceNativeClientImplAndroid::Resume Done");

	return 0;
}

int VueceNativeClientImplAndroid::Seek(int position)
{
	VueceLogger::Debug("VueceNativeClientImplAndroid::Seek");

	if (client->GetShell() != NULL)
	{
		return client->GetShell()->SeekMusic(position);
	}
	else
	{
		VueceLogger::Fatal("VueceNativeClientImplAndroid::Seek - Core client is null, abort now.");
	}

	VueceLogger::Debug("VueceNativeClientImplAndroid::Seek Done");

	return -1;
}
