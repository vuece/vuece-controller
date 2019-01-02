package com.vuece.vtalk.android.jni;

public interface VTalkListener {
	
	public static String DB_CACHE_DIR="/sdcard/vuece/db";
	public static String AUDIO_CACHE_DIR="/sdcard/vuece/tmp/audio";
	
    public static String PRESENCE_NONE     = "none";
    public static String PRESENCE_OFFLINE  = "offline";
    public static String PRESENCE_XA       = "xa";
    public static String PRESENCE_AWAY     = "away";
    public static String PRESENCE_DND      = "dnd";
    public static String PRESENCE_ONLINE   = "online";
    public static String PRESENCE_CHAT     = "chat";

	public static int SESSION_STATE_INIT = 0;
	public static int SESSION_STATE_SENTINITIATE = 1;       // sent initiate, waiting for Accept or Reject
	public static int SESSION_STATE_RECEIVEDINITIATE = 2;   // received an initiate. Call Accept or Reject
	public static int SESSION_STATE_SENTACCEPT =3;         // sent accept. begin connecting transport
	public static int SESSION_STATE_RECEIVEDACCEPT=4;     // received accept. begin connecting transport
	public static int SESSION_STATE_SENTMODIFY=5;         // sent modify, waiting for Accept or Reject
	public static int SESSION_STATE_RECEIVEDMODIFY=6;     // received modify, call Accept or Reject
	public static int SESSION_STATE_SENTREJECT=7;         // sent reject after receiving initiate
	public static int SESSION_STATE_RECEIVEDREJECT=8;     // received reject after sending initiate
	public static int SESSION_STATE_SENTREDIRECT=9;       // sent direct after receiving initiate
	public static int SESSION_STATE_SENTTERMINATE=10;      // sent terminate (any time / either side)
	public static int SESSION_STATE_RECEIVEDTERMINATE=11;  // received terminate (any time / either side)
	public static int SESSION_STATE_INPROGRESS=12;         // session accepted and in progress
	public static int SESSION_STATE_DEINIT=13;             // session is being destroyed
	
	public static int SUBSCRIPTION_TYPE_UNAVAILABLE=0;
	public static int SUBSCRIPTION_TYPE_SUBSCRIBE=1;
	public static int SUBSCRIPTION_TYPE_UNSUBSCRIBE=2;
	public static int SUBSCRIPTION_TYPE_SUBSCRIBED=3;
	public static int SUBSCRIPTION_TYPE_UNSUBSCRIBED=4;
	public static int SUBSCRIPTION_TYPE_INVITED=5;
	
	public static int FILESHARE_STATE_FS_NONE = 0;          // Initialization
	public static int FILESHARE_STATE_FS_OFFER = 1;         // Offer extended
	public static int FILESHARE_STATE_FS_TRANSFER = 2;      // In progress
	public static int FILESHARE_STATE_FS_COMPLETE = 3;      // Completed successfully
	public static int FILESHARE_STATE_FS_LOCAL_CANCEL = 4;  // Local side cancelled
	public static int FILESHARE_STATE_FS_REMOTE_CANCEL = 5; // Remote side cancelled (remote declined or remote canceled request)
	public static int FILESHARE_STATE_FS_FAILURE = 6;       // An error occurred during transfer
	public static int FILESHARE_STATE_FS_TERMINATED = 7;       // session terminated
	public static int FILESHARE_STATE_FS_RESOURCE_RELEASED = 8;       // extra state: resource released
	
	//see same definitions in vuecemscommon.h
	public static int MEDIA_PLAYER_STATE_STOPPED = 0;
	public static int MEDIA_PLAYER_STATE_BUFFERING = 1;
	public static int MEDIA_PLAYER_STATE_PLAYING = 2;
//	public static int MEDIA_PLAYER_STATE_PAUSED = 3;
	public static int MEDIA_PLAYER_STATE_SEEKING = 4;
	public static int MEDIA_PLAYER_STATE_STOPPING = 5;
	
	// jabber client callbacks
	public void onClientStateChanged(int event, int state);
	public void onRosterStatusUpdate(String jid, int priority, String status, int show, boolean hasPhone, boolean hasVideo, boolean hasCamera, boolean hasMuc, boolean hasShare, boolean hasControl, String iconHash, String hubName, String hubId);
	public void onSubscriptionRequest(String jid, int type);
	public void onVhubGetMessageReceived(String jid, String message);
	public void onVhubResultMessageReceived(String jid, String message);
	
	// hub management callbacks
	//public void onHubStateChanged(String jid, int state);
	//public void onProbeResponse(String jid, String data);
	//public void onBrowseResponse(String jid, String data);

	// network player callbacks
	public void onPreviewReceived(String path);
	public void onNetworkPlayerStateChanged(int event, int state);
	public void onMusicStreamingProgress(int second);
	public void onPlayingProgress(int second);
	
	public void onMessage(String jid, String message);
	public void onReceiveVCard(String jid, String fullName, String iconBase64String);
	public void onSessionStateChanged(int state);
	public void onRosterReceived(Object[] roster);
	
	public void onFileShareRequest(Object fileShareOption);
	public void onFileShareStateChanged(String fileShareId, int state);
	public void onFileShareStateBytesUpdate(String fileShareId, int percent);
	
	public void onStreamPlayerStateChanged(int state);
	public void onStreamPlayerProgress(int second);
}
