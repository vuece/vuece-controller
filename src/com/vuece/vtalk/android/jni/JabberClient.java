package com.vuece.vtalk.android.jni;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.BrowsingStartedEvent;
import com.vuece.controller.event.ClientStateChangedEvent;
import com.vuece.controller.event.HubExitedEvent;
import com.vuece.controller.event.HubOpenedEvent;
import com.vuece.controller.event.PlayerProgressEvent;
import com.vuece.controller.event.PlayerStateChangedEvent;
import com.vuece.controller.event.PreviewAvailableEvent;
import com.vuece.controller.event.SongListChangedEvent;
import com.vuece.controller.event.StreamingProgressEvent;
import com.vuece.controller.event.ToastMessageEvent;
import com.vuece.controller.model.ClientState;
import com.vuece.controller.model.DirectoryItem;
import com.vuece.controller.model.DisplayItem;
import com.vuece.controller.model.HubEntry;
import com.vuece.controller.model.NetworkPlayerEvent;
import com.vuece.controller.model.NetworkPlayerState;
import com.vuece.controller.model.ParentDirectoryItem;
import com.vuece.controller.model.SongItem;
import com.vuece.controller.service.ControllerService;
import com.vuece.controller.service.DBCacheHelper;
import com.vuece.controller.service.HubManager;
import com.vuece.vtalk.android.model.FileShareOption;
import com.vuece.vtalk.android.util.JabberUtils;
import com.vuece.vtalk.android.util.Log;

public class JabberClient implements Runnable, VTalkListener {

	private static JabberClient self;
	private static String TAG = "JabberClient";
	public static String SERVER_NAME="talk.google.com";
	public static int SERVER_PORT=5222;
	private static AudioTrack.OnPlaybackPositionUpdateListener audioTrackListener;
	private static void loadOptionalLibrary(String s) {
		try {
			System.loadLibrary(s);
		} catch (Throwable e) {
			Log.w(TAG, "Unable to load optional library lib" + s);
		}
	}

	static {
		System.loadLibrary("vtalk");
	}
	
	// jabber client functions
	public native int start(String username, String password, int authType);
	public native int logout();
	public native int getClientState();
	public native long initiate(VTalkListener listener, int loggingLevel, String deviceName, String appVersion);
	public native Object[] getRoster();
	public native int destroyClient();
	public native void sendChat(String jid, String message);
	public native void sendVCardRequest(String jid);
	public native void sendVHubMessage(String jid, String type, String message);
	public native void sendVHubPlayRequest(String jid, String type, String message, String targetUri);
	public native void addBuddy(String jid);
	public native void sendPresence(String show, String status);
	public native Object getCallRequest();
	public native void sendSubscriptionResponse(String jid, int type);
	
	// network player functions
	public native int getNetworkPlayerState();
	public native boolean isMusicStreaming();
	public native int getCurrentMusicStreamingProgress();
	public native int getCurrentPlayingProgress();
	public native int play(String jid, String uuid);
	public native int pause();
	public native int resume();
	public native int seek(int position);
	
	// this will return file share id
	public native String sendFile(String fileShareId, String jid, String pathname);
	public native void cancelFileShare(String fileShareId);
	public native void acceptFileShare(String fileShareId, String sampleRate, String folder, String filename);
	public native void declineFileShare(String fileShareId);
	//default download folder, can be set in config
	public native void setFileShareFolder(String path);
	//stream player control
//	public native void stopStreamPlayer(String fileShareId);
//	public native void resumeStreamPlayer(String fileShareId);
	
	private String username;
	private String password;
	private int authType;
	private final VTalkListener mListener;
	private long pointer;
	private ControllerService service;
	private String autoAccept;
	private Handler handler = new Handler();
	private HubManager hubManager;
	
	//data
	private HubEntry currentHubEntry;
	private DBCacheHelper dbCacheHelper;
    public String currentHubJid;
    // media streaming session id
    private String currentShareId;
    public SongItem currentSongItem;
    public ArrayList<SongItem> currentPlayList;
    public int currentPlayIndex;
    public int currentStartTime;
    private int[] currentPlayOrder;
    private ArrayList<ParentDirectoryItem> parentDirs;
    public ArrayList<DisplayItem> currentList;
    private boolean browsing;
    public boolean shuffle;
    public int repeatMode;  // 0=no repeat; 1=repeat one song; 2=repeat list
    private boolean autoNext=true;
    private SongItem toPlaySongItem;
	
	public static JabberClient getInstance(){
		return self;
	}
	
	public JabberClient(ControllerService service){
		mListener=this;
		hubManager=new HubManager();
		// init data
        currentList=new ArrayList<DisplayItem>();
        currentPlayList=new ArrayList<SongItem>();
        currentPlayIndex=-1;
        currentStartTime=0;
        parentDirs=new ArrayList<ParentDirectoryItem>();
        dbCacheHelper=new DBCacheHelper();
		this.service=service;
		self=this;
	}

	public void setCredential(String username, String password, int type){
		this.username=username;
		this.password=password;
		this.authType=type;
	}
	public boolean isConnected(){
		Log.d(TAG, "client state:"+getClientState());
		return getClientState()==ClientState.ONLINE.ordinal();
	}
	
	public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		if (getClientState()==ClientState.OFFLINE.ordinal()) {
			try {
				Socket socket=new Socket(SERVER_NAME, SERVER_PORT);
				socket.close();
				Log.d(TAG, "starting jingle with user :"+username+" and auth type:"+authType);
				hubManager.username=username;
				start(username,password,authType);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.w(TAG, SERVER_NAME+" cannot be resolved.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.w(TAG, "Cannot connect to "+SERVER_NAME+":"+SERVER_PORT);
			}
		}
	}
	
	public void destroy() {
		destroyClient();
		
	}
	
	// data access
	public HubManager getHubManager() {
		return hubManager;
	}
	public boolean isHubbed(){
		return this.currentHubJid!=null;
	}
	public void clearCurrentState() {
		if (getNetworkPlayerState()==NetworkPlayerState.PLAYING.ordinal()) {
			Log.d(TAG, "clearCurrentState: before pause");
			pause();
			Log.d(TAG, "clearCurrentState: after pause");
		}
		currentHubJid=null;
		currentSongItem=null;
		currentPlayList.clear();
        currentPlayIndex=-1;
        currentStartTime=0;
		parentDirs.clear();
	}
	public void clearVhub() {
		browsing=false;
		clearCurrentState();
	}
	
	public void enterHub(final String jid){
		final HubEntry newHub=hubManager.getHubByJid(jid);
		if (newHub.accessable==HubEntry.ACCESSIBLE_NO) {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("You are not allowed to access hub "+newHub.name));
				}
			});
			return;
		}
		if (newHub.dbChecksum==null) {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Hub "+newHub.name+" is not ready."));
				}
			});
			return;
		}
		clearVhub();
		currentHubJid=jid;
		browseMusicDirectory(null);
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new HubOpenedEvent());
			}
		});
	}
	public String getCurrentHubName(){
		return hubManager.getHubByJid(currentHubJid).getName();
	}
	
	// internal control
//	public void stopStreaming() {
//		if (currentStreamingState==VTalkListener.FILESHARE_STATE_FS_TRANSFER) cancelFileShare(currentShareId);
//	}
//	
//    public void startPlaying(int startTime){
//    	currentPlayerState=VTalkListener.MEDIA_PLAYER_STATE_BUFFERING;
//    	Log.d(TAG, "currentSongItem:"+currentSongItem);
//    	toPlaySongItem=currentSongItem;
//        sendPlayGetMessage(currentHubJid, "music", "start", currentSongItem.getUri(), startTime, true);
//    }
//    
//    public void stopPlaying(){
//		if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_PLAYING) {
//			currentPlayerState=VTalkListener.MEDIA_PLAYER_STATE_STOPPING;
//			stopStreamPlayer(currentShareId);
//		}
//	}
//	
//    public void resumePlaying(){
//		if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_STOPPED){ 
//			if (currentPlayerProgress==currentStreamingProgress&&currentStreamingState==VTalkListener.FILESHARE_STATE_FS_RESOURCE_RELEASED) 
//				currentPlayerProgress=0;
//			resumeStreamPlayer(String.valueOf(currentPlayerProgress));
//		}
//	}
//    
//    public void mediaSeek(final int seekingTime) {
//		Log.d(TAG, "seekingTime:"+seekingTime+";currentStartTime:"+currentStartTime);
//		if (seekingTime>=currentSongItem.getLength()) {
//			Log.e(TAG, "cannot seek over the end of song.");
//			return;
//		}
////		if (!commandExecutor.seek(seekingTime, resume)) {
//		if (seek(seekingTime)==-1) {
//			handler.post(new Runnable(){
//				public void run() {
//					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
//				}
//			});
//		}
//	}
	private void showCurrentPlayOrder(){
		StringBuffer sb=new StringBuffer();
		for (int j:currentPlayOrder) sb.append(j).append(" ");
		Log.d(TAG, "new order:"+sb.toString());
	}
	
//	public int getCurrentStreamingProgress() {
//		return currentStreamingProgress;
//	}
	public void refreshCurrentDirectory(){
		if (parentDirs.size()==0) 
			browseMusicDirectory(null);
		else
			browseMusicDirectory(parentDirs.get(parentDirs.size()-1));
	}
	public void browseMusicDirectory(DirectoryItem dir) {
		if (browsing) return;
		this.browsing=true;
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new BrowsingStartedEvent());
			}
		});
		Log.d(TAG, "start:parentDirs:"+parentDirs);
		
		// if the query is cached, use it
		HubEntry hub=hubManager.getHubByJid(currentHubJid);
		String content=dbCacheHelper.getCache(hub.id, hub.dbChecksum, dir==null?null:dir.getUri());
		if (content!=null) {
			Log.d(TAG, "local browsing:"+(dir==null?"ROOT":dir.getTitle()));
			try {
				JSONObject object = new JSONObject(content);
				List<DisplayItem> newList=this.convertMessageToDisplayItems(object);
				if (dir==null){
					parentDirs.clear();
					parentDirs.add(new ParentDirectoryItem(getCurrentHubName(), "", 0, 0));
//					parentDirs.add(new ParentDirectoryItem(ParentDirectoryItem.ROOT_NAME, "", 0, 0));
				}else

				if (dir!=null&&!dir.getUri().equalsIgnoreCase(parentDirs.get(parentDirs.size()-1).getUri())) {
					if (dir instanceof ParentDirectoryItem) {
						parentDirs.remove(parentDirs.size()-1);
					}else
						parentDirs.add(new ParentDirectoryItem(dir.getTitle(), dir.getUri(), dir.getNumDirs(), dir.getNumSongs()));
				}
				
				currentList.clear();
				if (parentDirs.size()>1) currentList.add(parentDirs.get(parentDirs.size()-2));
				currentList.addAll(newList);
				
				sortCurrentList();
				// allow browsing again
				browsing=false;
				// notify UI
				handler.post(new Runnable(){
					public void run() {
						BusProvider.getInstance().post(new SongListChangedEvent());
					}
				});
				Log.d(TAG, "end:parentDirs:"+parentDirs);
				return;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "can't parse database cache:"+content);
			}
		}
		Log.d(TAG, "remote browsing:"+(dir==null?null:dir.getTitle()));
		if (dir==null){
			parentDirs.clear();
			parentDirs.add(new ParentDirectoryItem(getCurrentHubName(), "", 0, 0));
//				parentDirs.add(new ParentDirectoryItem(ParentDirectoryItem.ROOT_NAME, "", 0, 0));
	 		sendBrowseGetMessage(currentHubJid, "music", null);
		}else
			sendBrowseGetMessage(currentHubJid, "music", dir.getUri());

		if (dir!=null&&!dir.getUri().equalsIgnoreCase(parentDirs.get(parentDirs.size()-1).getUri())) {
			if (dir instanceof ParentDirectoryItem) {
				parentDirs.remove(parentDirs.size()-1);
			}else
				parentDirs.add(new ParentDirectoryItem(dir.getTitle(), dir.getUri(), dir.getNumDirs(), dir.getNumSongs()));
		}

		Log.d(TAG, "end:parentDirs:"+parentDirs);
//		if (currentStreamingState==VTalkListener.FILESHARE_STATE_FS_TRANSFER) {
//			//stop and resume streaming to allow message to be processed
////			client.
//		}
	}
	public int playSong(SongItem songItem){
		if (songItem==null) return -1;
		if (currentSongItem!=null&&currentSongItem.getUri()==songItem.getUri()){
			Log.d(TAG, "song-to-play is current song.");
			if (getNetworkPlayerState()==NetworkPlayerState.PLAYING.ordinal()||getNetworkPlayerState()==NetworkPlayerState.BUFFERING.ordinal()) 
				return 0;
			if (getNetworkPlayerState()==NetworkPlayerState.IDLE.ordinal())
				resume();
		}
		currentSongItem=songItem;
    	//populate play list
    	this.currentPlayList.clear();
    	int i=0;
    	currentPlayOrder=new int[currentList.size()];
    	for (DisplayItem displayItem:currentList){
    		if (displayItem instanceof SongItem) {
    			currentPlayList.add((SongItem) displayItem);
    			if (displayItem.getUri().equalsIgnoreCase(songItem.getUri()))
    				currentPlayIndex=i;
    			currentPlayOrder[i]=i;
    			i++;
    		}
    	}
    	Log.d(TAG, "added "+currentPlayList.size()+" to play list; current playing no. "+currentPlayIndex);
		showCurrentPlayOrder();
    	//this.currentSongItem=toPlaySongItem;
		return play(currentHubJid, songItem.getUri());
	}

	public int playPause() {
		Log.d(TAG, "PlayPauseButtonPressed: "+getNetworkPlayerState());
		if (currentSongItem==null) return -1;
		
		if (getNetworkPlayerState()==NetworkPlayerState.IDLE.ordinal()) {
			resume();
		}
//		else if (getNetworkPlayerState()==NetworkPlayerState.PLAYING.ordinal()) {
//			pause();
//		}
		else if (getNetworkPlayerState()==NetworkPlayerState.WAITING.ordinal()) {
			return -1;
		}
		else  {
			pause();
		}
		return 0;
	}

	public void resetCounters() {
		this.currentSongItem=null;
	}

	public String getCurrentDirTitle() {
		if (parentDirs.size()==0) return getCurrentHubName(); //ParentDirectoryItem.ROOT_NAME;
		return parentDirs.get(parentDirs.size()-1).getTitle();
	}
	
	public void shuffle(){
		Log.d(TAG, "shuffle");
		
		this.shuffle=!this.shuffle;
		if (shuffle) {
			// shuffle play order
			List<Integer> numList=new ArrayList<Integer>();
			for (int i:currentPlayOrder) numList.add(i);
			currentPlayOrder[0]=numList.remove(currentPlayIndex);
			for (int i=1;i<currentPlayOrder.length;i++){
				currentPlayOrder[i]=numList.remove((int)(Math.random()*numList.size()));
			}
			showCurrentPlayOrder();
		} else {
			int z=0;
			for (int i:currentPlayOrder) {
				currentPlayOrder[z]=z;
				z++;
			}
		}
	}
	public void repeat() {
		Log.d(TAG, "repeat");
		this.repeatMode=(this.repeatMode+1)%3;
		
	}
    public int playNext(){
    	if (currentPlayList==null||currentPlayList.size()==0) return -1;
    	Log.d(TAG, "playNext");
    	currentPlayIndex++;
    	if (currentPlayIndex==currentPlayList.size()) currentPlayIndex=0;
    	Log.d(TAG, "play list has "+currentPlayList.size()+" songs; current playing no. "+currentPlayIndex);
    	this.currentSongItem=currentPlayList.get(currentPlayOrder[currentPlayIndex]);
//    	startPlaying(0);
    	return play(currentHubJid, currentSongItem.getUri());
    }
    public int playPrevious(){
    	if (currentPlayList==null||currentPlayList.size()==0) return -1;
    	currentPlayIndex--;
    	if (currentPlayIndex<0) currentPlayIndex=currentPlayList.size()-1;
    	Log.d(TAG, "play list has "+currentPlayList.size()+" songs; current playing no. "+currentPlayIndex);
    	this.currentSongItem=currentPlayList.get(currentPlayOrder[currentPlayIndex]);
//    	startPlaying(0);
    	return play(currentHubJid, currentSongItem.getUri());
    }

    public void seekingStart(){
    	//pause();
    }
    public int seekingStop(int currentProgress){
		Log.d(TAG, "user stopped moving (auto-pause):"+currentProgress+" seekingTime:"+currentProgress);
		if (currentProgress>=currentSongItem.getLength()) {
			Log.e(TAG, "cannot " +
					" over the end of song.");
			return -1;
		}
		return seek(currentProgress);
    }
    
	public void cleanBeforeLogout(){
		if (dbCacheHelper!=null)
			dbCacheHelper.closeDB();
	}

	public void sendBrowseGetMessage(String jid, String type, String uri){
		sendVHubMessage(jid, "get", "{'action':'browse','category':'"+type+"'"+(uri==null||uri.length()==0?"":",'uri':'"+uri+"'")+"}");
	}
	
	public void sendMockVhubMessage(String jid, String message){
		if (message.equalsIgnoreCase("browse-root"))
			onVhubResultMessageReceived(jid, "{'action':'browse','reply':'ok','category':'music',items:[{'name':'mp3-dir','type':'dir','uri':'d:/mp3'}]}");
	}
	public void sendPlayGetMessage(String jid, String category, String control, String uri, int start, boolean needPreview) {
		this.autoAccept=jid;
//		sendVHubMessage(jid, "get", "{'action':'play','category':'"+category+"','control':'"+control+"', 'uri':'"+uri+"', 'start':'"+start+"', 'need_preview': "+(needPreview?"'1'":"'0'")+"}");
		String msg = "{'action':'play','category':'"+category+"','control':'"+control+"', 'uri':'"+uri+"', 'start':'"+start+"', 'need_preview': "+(needPreview?"'1'":"'0'")+"}";
		
		sendVHubPlayRequest(jid, "get", msg, uri);
	}

	//listener methods, invoked from C layer
	public void onClientStateChanged(final int event, final int state) {
		Log.d(this.getClass().getName(), "event: "+event+"; client state changed to "+state);
//		if (event==VTalkListener.CLIENT_EVENT_LOGOUT_OK) {
////			commandExecutor.setRunning(false);
//			clearVhub();
//		}
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new ClientStateChangedEvent(event, state));
			}
		});
		//service.notifyClientStateChange(state);
	}
	public void onMessage(String jid, String message) {
		Log.d(this.getClass().getName(), "jid:"+jid+";message:"+message);
	}
	public void onReceiveVCard(final String jid, final String fullName,
			final String iconBase64String) {
		Log.d("onReceiveVCard", "jid:"+jid+";FN:"+fullName+";icon size:"+iconBase64String.getBytes().length);
	}
	public void onRosterStatusUpdate(final String jid, final int priority, final String status,
			final int show, final boolean hasPhone, final boolean hasVideo, final boolean hasCamera,
			final boolean hasMuc, final boolean hasShare, final boolean hasControl, final String iconHash, final String hubName, final String hubId) {
		Log.d(TAG, "onStatusUpdate:"+jid+";priority="+priority+";status="+status+";show="+show+";hasPhone:"+hasPhone+";hasCamera:"+hasCamera+";hasShare:"+hasShare+";hasControl:"+hasControl+";hubName:"+hubName+";hubId:"+hubId);
		if (hasControl) {
			Bundle data = new Bundle();
			data.putString("type", "add");
			data.putString("jid", jid);
			data.putString("name", hubName);
			data.putString("id", hubId);
			data.putByte("accessible", HubEntry.ACCESSIBLE_UNKNOWN);
		    Message msg=new Message();
			msg.setData(data);
			hubManager.sendMessage(msg);
			Log.d(TAG, "device:"+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" "+android.os.Build.PRODUCT+" "+android.os.Build.BRAND);
			sendVHubMessage(jid, "get", "{'action':'probe', 'device':'"+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"', 'version': '"+getVersion(service)+"'}");
		}
	}
	private int getVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionCode;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }
	public void onSessionStateChanged(final int state) {
		Log.d(TAG, "}}}}}}}}}}}}}}}}}}}}}}}session state:" + state);
	}

	public void initiateClient(){
		
		//TODO - Define your own java constants according to the following global logging level
		//definition
		//		#define VUECE_LOG_LEVEL_DEBUG 	0
		//		#define VUECE_LOG_LEVEL_INFO 	1
		//		#define VUECE_LOG_LEVEL_WARN 	2
		//		#define VUECE_LOG_LEVEL_ERROR 	3
		//		#define VUECE_LOG_LEVEL_NONE	4

		pointer=initiate(this, 3, android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL, String.valueOf(getVersion(service)));
	}

	public void onSubscriptionRequest(final String jid, final int type) {
		Log.d(TAG, "onSubscriptionRequest:"+jid+";type:"+type);
//		service.onSubscriptionRequest(jid, type);
		if (type==0&&hubManager.getHubByJid(jid)!=null) {
			Log.d(TAG, "hub "+jid+" logged out, clear it");
			Bundle data = new Bundle();
			data.putString("type", "remove");
			data.putString("jid", jid);
		    Message msg=new Message();
			msg.setData(data);
			hubManager.sendMessage(msg);
			handler.post(new Runnable(){
				public void run() {
					clearVhub();
					BusProvider.getInstance().post(new HubExitedEvent());
				}
			});
		}
	}
	

	public void onRosterReceived(Object[] roster) {
		Log.d(TAG, "onRosterReceived:"+roster);
	}
	
	

	// JNI callback methods
	@Override
	public void onFileShareStateChanged(String fileShareId, final int state) {
		Log.d(TAG, "file share state ("+fileShareId+") changed to "+state);
	}

	@Override
	public void onFileShareStateBytesUpdate(String fileShareId, final int percentage) {
		Log.d(TAG, "file share ("+fileShareId+") percentage update: "+percentage);
	}

	@Override
	public void onFileShareRequest(Object fileShareOption) {
		Log.d(TAG, "received file share request: "+fileShareOption);
		if (!(fileShareOption instanceof FileShareOption)) return;
		currentShareId=((FileShareOption)fileShareOption).getId();
		int type=((FileShareOption)fileShareOption).type;
		if (type==FileShareOption.T_FILE) {
			this.declineFileShare(currentShareId);
		} else if (type==FileShareOption.T_MUSIC) {
		
		}
	}
	@Override
	public void onVhubGetMessageReceived(String jid, String message) {
		Log.d(TAG, "onVhubGetMessageReceived: from "+jid+" >> "+message);
		try {
			JSONObject object = new JSONObject(message);
			String action=object.getString("action");
			if ("probe".equals(action)) {
				sendVHubMessage(jid,"result","{'action':'"+action+"','reply':'error', 'resson':'this is not a hub.'}");
			} else if ("notification".equals(action)){
				String category=object.getString("category");
				String type=object.getString("type");
				if ("streaming-resource-released".equals(type)){
//					currentStreamingState=VTalkListener.FILESHARE_STATE_FS_RESOURCE_RELEASED;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "onVhubGetMessageReceived: message received is not a valid json message");
			e.printStackTrace();
		}
	}
	@Override
	public void onVhubResultMessageReceived(final String jid, final String message) {
		Log.d(TAG, "onVhubResultMessageReceived: from "+jid+" >> "+message);
		try {
			JSONObject object = new JSONObject(message);
			String action=object.getString("action");
			Log.d(TAG,"action:"+action);
			String reply=object.getString("reply");
			if ("probe".equals(action)) {
				if ("ok".equalsIgnoreCase(reply)) {
//					JSONArray functions=object.getJSONArray("categories");
//					for (int i = 0; i < functions.length(); i++) {
//						addFunctionToHub(jid, functions.getString(i));
//					}
					HubEntry hub=hubManager.getHubByJid(jid);
					Bundle data = new Bundle();
					data.putString("type", "add");
					data.putString("jid", jid);
					data.putByte("accessible", HubEntry.ACCESSIBLE_YES);
					// begin test code
	//				dbSynced=true;
	//				dbCacheHelper=new DBCacheHelper(service);
					// end test code
					if (object.has("db-cache-chksum")) {
						String chksum=object.getString("db-cache-chksum");
						data.putString("db-checksum", chksum);
						dbCacheHelper.cleanup(hub.id, chksum);
					}
				    Message msg=new Message();
					msg.setData(data);
					hubManager.sendMessage(msg);
					if (currentHubJid==null&&username.equals(JabberUtils.normalizeJid(jid)))
						handler.post(new Runnable(){
							public void run() {
								enterHub(jid);
							}
						});
				}else{
					Bundle data = new Bundle();
					data.putString("type", "add");
					data.putString("jid", jid);
					data.putByte("accessible", HubEntry.ACCESSIBLE_NO);
				    Message msg=new Message();
					msg.setData(data);
					hubManager.sendMessage(msg);
				}
//                    	if (isControllerActivityShown()) ((ControllerActivity)currentActivity).refreshHubList();
			}
			if (!"ok".equalsIgnoreCase(reply)) {
				final String msg=object.getString("reason");
				handler.post(new Runnable(){
					public void run() {
						BusProvider.getInstance().post(new ToastMessageEvent(msg));
					}
				});
        		return;
			}
			if ("browse".equals(action)) {
				HubEntry hub=hubManager.getHubByJid(jid);
				String uri=object.getString("uri");
				dbCacheHelper.addCache(hub.id, hub.dbChecksum, uri, message);
				currentList.clear();
				if (parentDirs.size()>1) currentList.add(parentDirs.get(parentDirs.size()-2));
				currentList.addAll(convertMessageToDisplayItems(object));
				sortCurrentList();
				// allow browsing again
				browsing=false;
				// notify UI
				handler.post(new Runnable(){
					public void run() {
						BusProvider.getInstance().post(new SongListChangedEvent());
					}
				});
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "onVhubResultMessageReceived: message received is not a valid json message");
			e.printStackTrace();
		}
	}
	
	private List<DisplayItem> convertMessageToDisplayItems(JSONObject object) throws JSONException {
		JSONObject itemObj;
		DisplayItem displayItem;
		String filename, title="Unknown", artist="Unknown", album="Unknown";
		JSONArray items=object.getJSONArray("list");
		List<DisplayItem> list=new ArrayList<DisplayItem>();
		for (int i = 0; i < items.length(); i++) {
			itemObj=items.getJSONObject(i);
			Log.d(TAG,"name:"+itemObj.getString("name")+";uri:"+itemObj.getString("uri"));
			try {
				filename=new String (Base64.decode(itemObj.getString("name"), Base64.DEFAULT), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				filename="Unknown";
			}
			if (itemObj.has("type")&&"dir".equalsIgnoreCase(itemObj.getString("type"))) {
//						item.setType(MediaItem.TYPE_DIR);
				displayItem=new DirectoryItem(filename,itemObj.getString("uri"),itemObj.optInt("num-dirs",0),itemObj.optInt("num-songs",0));
			}else{
//						if ("music".equalsIgnoreCase(category)) item.setType(MediaItem.TYPE_MUSIC);
//						if ("video".equalsIgnoreCase(category)) item.setType(MediaItem.TYPE_VIDEO);
				try {
					if (itemObj.getString("title")!=null) 
						title=new String (Base64.decode(itemObj.getString("title"), Base64.DEFAULT), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					if (itemObj.getString("artist")!=null) 
						artist=new String (Base64.decode(itemObj.getString("artist"), Base64.DEFAULT), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					if (itemObj.getString("album")!=null) 
						album=new String (Base64.decode(itemObj.getString("album"), Base64.DEFAULT), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(TAG, "filename:"+filename+";title:"+title+";artist:"+artist+";album:"+album);
				displayItem=new SongItem(filename,itemObj.getString("uri"),itemObj.getInt("length"),itemObj.getInt("size"),title,artist,album,itemObj.getInt("samplerate"));
			}
//					if (itemObj.has("length")) item.setLength(itemObj.getInt("length"));
//					if (itemObj.has("size")) item.setSize(itemObj.getInt("size"));
//					if (itemObj.has("artist")) item.setArtist(itemObj.getString("artist"));
//					currentList.add(item);
			list.add(displayItem);
		}
		return list;
	}
	private void sortCurrentList(){
		Collections.sort(currentList, new Comparator<DisplayItem>(){
			@Override
			public int compare(DisplayItem arg0, DisplayItem arg1) {
				Log.d(TAG, "comparing:"+arg0.getTitle()+" vs "+arg1.getTitle());
				if (arg0 instanceof ParentDirectoryItem)
					return -1;
				if (arg1 instanceof ParentDirectoryItem)
					return 1;
				if (!(arg0 instanceof SongItem)&&(arg1 instanceof SongItem))
					return -1;
				if (!(arg1 instanceof SongItem)&&(arg0 instanceof SongItem))
					return 1;
				if ((arg1 instanceof SongItem)&&(arg0 instanceof SongItem))
					return ((SongItem)arg0).getName().compareToIgnoreCase(((SongItem)arg1).getName());
				return arg0.getTitle().compareToIgnoreCase(arg1.getTitle());
			}
		});
	}
	
	@Override
	public void onStreamPlayerStateChanged(final int state) {
		Log.d(TAG, "onStreamPlayerStateChanged: "+state);
//		this.currentPlayerState = state;
//		if (autoNext&&currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_STOPPED) { //currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_PAUSED||
//			Log.d(TAG, "currentSongItem:"+currentSongItem);
//			Log.d(TAG, "currentPlayerProgress:"+currentPlayerProgress);
//			Log.d(TAG, "currentStreamingProgress:"+currentStreamingProgress);
//			Log.d(TAG, "currentStreamingState: "+currentStreamingState);
////			if (currentSongItem!=null&&currentPlayerProgress>=currentStreamingProgress&&currentStreamingState==VTalkListener.FILESHARE_STATE_FS_TERMINATED) {
////				Log.d(TAG, "Player reaches the end of song, play next one if necessary. repeatMode:"+this.repeatMode+"; shuffle:"+this.shuffle);
////				currentPlayerProgress=0;
////				if (this.repeatMode==1) {
//////					mediaSeek(0, true);
////					this.currentPlayerProgress=0;
////					commandExecutor.resume();
////				} else if (this.repeatMode==2||this.shuffle) {
//////					playNext();
////					commandExecutor.next();
////				}
////			}
//		}
//		handler.post(new Runnable(){
//			public void run() {
//				BusProvider.getInstance().post(new PlayerStateChangedEvent(state));
//			}
//		});
	}
	@Override
	public void onStreamPlayerProgress(final int second) {
		Log.d(TAG, "onStreamPlayerProgress: "+second);
//		service.setCurrentPlayerProgress(second);
//		this.currentPlayerProgress = second;
//		if (currentStreamingState==VTalkListener.FILESHARE_STATE_FS_RESOURCE_RELEASED&&currentPlayerProgress>=currentStreamingProgress){
//			Log.d(TAG, "End of playing, need to stop player to release resources. and work out what to do next. repeatMode:"+this.repeatMode+"; shuffle:"+this.shuffle);
////			stopPlaying();
//			if (autoNext&&currentSongItem!=null&&currentPlayerProgress>=currentStreamingProgress&&currentStreamingState==VTalkListener.FILESHARE_STATE_FS_RESOURCE_RELEASED) {
////					Log.d(TAG, "Player reaches the end of song, play next one if necessary. repeatMode:"+this.repeatMode+"; shuffle:"+this.shuffle);
////				currentPlayerProgress=0;
//				if (this.repeatMode==1) {
////						mediaSeek(0, true);
////					this.currentPlayerProgress=0;
////					commandExecutor.resume();
//					commandExecutor.seek(0, true);
//				} else if (this.repeatMode==2||this.shuffle) {
////						playNext();
//					commandExecutor.next();
//				} else
//					commandExecutor.pause();
//			}else
//				commandExecutor.pause();
////			this.currentPlayerProgress = 0;
//		}
//		handler.post(new Runnable(){
//			public void run() {
//				BusProvider.getInstance().post(new PlayerProgressEvent(second));
//			}
//		});

	}
	@Override
	public void onPreviewReceived(final String path){
		Log.d(TAG, "onPreviewReceived:"+path);
    	this.currentSongItem.setPreviewPath(path);
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new PreviewAvailableEvent(path));
			}
		});
		// accept to start streaming
//		acceptFileShare(currentShareId, String.valueOf(currentSongItem.getSampleRate()), VTalkListener.AUDIO_CACHE_DIR, "preview.jpg");
	}
	@Override
	public void onNetworkPlayerStateChanged(final int event, final int state) {
		Log.d(TAG, "onNetworkPlayerStateChanged: event "+event+" state "+state);
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new PlayerStateChangedEvent(event, state));
				if (event==NetworkPlayerEvent.END_OF_SONG.getValue()) {
					if (JabberClient.this.repeatMode==1) {
						seek(0);
					} else if (JabberClient.this.repeatMode==2||JabberClient.this.shuffle) {
						playNext();
					}
				} else if (event==NetworkPlayerEvent.MEDIA_FILE_NOT_FOUND.getValue()) {
					if (JabberClient.this.repeatMode==2||JabberClient.this.shuffle) {
						playNext();
					}
				}
			}
		});
	}
	@Override
	public void onMusicStreamingProgress(final int second) {
		Log.d(TAG, "onMusicStreamingProgress: "+second);
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new StreamingProgressEvent(second));
			}
		});
	}
	@Override
	public void onPlayingProgress(final int second) {
		Log.d(TAG, "onPlayingProgress: "+second);
		handler.post(new Runnable(){
			public void run() {
				BusProvider.getInstance().post(new PlayerProgressEvent(second));
			}
		});
	}
}
