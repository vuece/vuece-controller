#ifndef _VUECE_NATIVE_CLIENT_IMPL_ANDROID
#define _VUECE_NATIVE_CLIENT_IMPL_ANDROID

extern "C"
{
	#include "stdint.h"
}

#include "talk/xmpp/xmppengineimpl.h"
#include "talk/base/sigslot.h"
#include "talk/base/thread.h"
#include "talk/base/scoped_ptr.h"

#include "VueceNativeInterface.h"
#include "VueceCoreClient.h"
#include "jni.h"
#include "VueceGlobalSetting.h"

class VueceKernelShell;
class VueceNativeClientImplAndroid : public vuece::VueceNativeInterface{

public:
	VueceNativeClientImplAndroid(vuece::InitData* init_data);

	virtual ~VueceNativeClientImplAndroid();

	void runTest();

	//log in
	virtual int Start(const char* name, const char* pwd, const int auth_type);
	virtual int LogOut(void);

	//log in state
	virtual vuece::ClientState  GetClientState(void);
	virtual void OnClientStateChanged(vuece::ClientEvent event, vuece::ClientState state);

	//hub messages
	virtual void 	OnVHubGetMessageReceived(const buzz::Jid& jid, const std::string& message);
	virtual void 	OnVHubResultMessageReceived(const buzz::Jid& jid, const std::string& message);
	virtual int 	SendVHubMessage(const std::string& to, const std::string& type,const std::string& message);
	virtual int 	SendVHubPlayRequest(const std::string& to, const std::string& type,const std::string& message, const std::string& uri);


	//roster management
	virtual void OnRosterStatusUpdate(const buzz::Status& status);
	virtual void OnRosterReceived(const buzz::XmlElement* stanza);
	virtual void OnRosterSubRespReceived(VueceRosterSubscriptionMsg* m);
	virtual void SendSubscriptionMessage(const std::string& to, int type);
	virtual void AddBuddy(const std::string& jid);
	virtual void SendPresence(const std::string& status, const std::string& signature);
	virtual void SendPresence(const std::string& status);
	virtual void SendSignature(const std::string& sig);
	virtual vuece::RosterMap* GetRosterMap();

	//music playing
	virtual vuece::NetworkPlayerState  GetNetworkPlayerState(void);
	virtual void OnNetworkPlayerStateChanged(vuece::NetworkPlayerEvent e, vuece::NetworkPlayerState s);
	virtual void OnStreamPlayerStateChanged(const std::string& share_id, int state);

//	virtual void ResumeStreamPlayer(const std::string& pos);
	virtual bool IsMusicStreaming(void);
	virtual int  GetCurrentPlayingProgress(void);

	//file share/streaming
	virtual void AcceptFileShare(const std::string& share_id,const std::string& target_folder, const std::string& target_file_name);
	virtual void DeclineFileShare(const std::string& share_id);
	virtual void CancelFileShare(const std::string& share_id);

	virtual void TestSendFile();

	//network player
	virtual int Play(const std::string &jid, const std::string &song_uuid);
	virtual int Pause();
	virtual int Resume();
	virtual int Seek(const int position);

	//share events
	virtual void OnFileShareRequestReceived(const std::string& share_id, const buzz::Jid& target, int type,  const std::string& fileName, int size, bool needPreview);
	virtual void OnFileShareProgressUpdated(const std::string& share_id, int percent);
	virtual void OnMusicStreamingProgressUpdated(const std::string& share_id, int progress);
	virtual int  GetCurrentMusicStreamingProgress(const std::string& share_id);
	virtual void OnFileSharePreviewReceived(const std::string& share_id, const std::string& path, int w, int h);
	virtual void OnFileShareStateChanged(const std::string& remote_jid,  const std::string& share_id, int state);

	//call
	virtual cricket::CallOptions* GetCurrentCallOption(void);
	virtual buzz::Jid GetCurrentJidInCall(void);

	//file system
	virtual void OnFileSystemChanged(void);

	virtual void OnRemoteDeviceActivity(VueceStreamingDevice* d);

	void OnXmmpSocketClosedEvent(int err);

	//phone call
	void OnCallSessionState(const int state);

	void OnVueceEvent(int code, const char* jid, const char* other);

#ifdef ENABLE_PHONE_ENGINE
	void HangUpCall();
	void PlaceVoiceCall(const char* jid, const char* sid);
	void PlaceVideoCall(const char* jid, const char* sid);
	void RejectCall();
	void AcceptCall(void);
#endif

#ifdef CHAT_ENABLED
	void SendChat(const std::string& to, const std::string& msg);
	void OnChatMessageReceived(const buzz::Jid&, const std::string&);
#endif

#ifdef VCARD_ENABLED
	void SendVCardRequest(const std::string& to);
	void OnRosterVCardReceived(const buzz::Jid&, const std::string&, const std::string&);
#endif

	void SendFile(
			const std::string& share_id,
			const std::string& jid,
			const std::string& pathname,
			const std::string& width,
			const std::string& height,
			const std::string& preview_file_path,
			const std::string& start_pos,
			const std::string& need_preview
			);	

	void HandleSignInSuccess(void);
	void LogCurrentClientState(void);

public:

	//command with 2 parameter
	sigslot::signal2<int, const char*> SigVueuceCommandMessage;

};

#endif
