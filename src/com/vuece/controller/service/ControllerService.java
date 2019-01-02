package com.vuece.controller.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.squareup.otto.Subscribe;
import com.vuece.controller.R;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.core.ControllerApplication;
import com.vuece.controller.event.PlayerStateChangedEvent;
import com.vuece.controller.event.ToastMessageEvent;
import com.vuece.controller.model.AuthType;
import com.vuece.controller.model.ClientState;
import com.vuece.controller.model.DirectoryItem;
import com.vuece.controller.model.DisplayItem;
import com.vuece.controller.model.NetworkPlayerState;
import com.vuece.controller.model.SongItem;
import com.vuece.controller.ui.GetTokenTask;
import com.vuece.controller.ui.MusicPlayerActivity;
import com.vuece.controller.ui.PlaySongTask;
import com.vuece.vtalk.android.jni.JabberClient;
import com.vuece.vtalk.android.util.JabberUtils;
import com.vuece.vtalk.android.util.Log;

public class ControllerService extends Service {
	
    public static final String ACTION_REMOTE_GENERIC = "com.vuece.controller.remote.";
    public static final String ACTION_REMOTE_BACKWARD = "com.vuece.controller.remote.Backward";
    public static final String ACTION_REMOTE_PLAY = "com.vuece.controller.remote.Play";
    public static final String ACTION_REMOTE_PLAYPAUSE = "com.vuece.controller.remote.PlayPause";
    public static final String ACTION_REMOTE_PAUSE = "com.vuece.controller.remote.Pause";
    public static final String ACTION_REMOTE_STOP = "com.vuece.controller.remote.Stop";
    public static final String ACTION_REMOTE_FORWARD = "com.vuece.controller.remote.Forward";
    public static final String ACTION_NETWORK_CONNECTED = "com.vuece.controller.network.Connected";
    public static final String ACTION_NETWORK_DISCONNECTED = "com.vuece.controller.network.Disconnected";

    public static final String PREFS_NAME = "VuecePrefsFile";
    
    public static int FLAG_RECEIVER_RINGING = 800;
	
	private static String TAG = "vuece/ControllerService";
	public static String SERVER_NAME="talk.google.com";
	public static int SERVER_PORT=5222;
	private JabberClient client;
	private PlaySongTask playSongTask;
	
    private WakeLock mWakeLock;
    private WifiLock wifiLock;
    private Toast toast;
    
//    private Stack<ArrayList<DisplayItem>> listStack;
    private long mWidgetPositionTimestamp = Calendar.getInstance().getTimeInMillis();
    private ComponentName mRemoteControlClientReceiverComponent;
    /**
     * RemoteControlClient is for lock screen playback control.
     */
    private RemoteControlClient mRemoteControlClient = null;
    private RemoteControlClientReceiver mRemoteControlClientReceiver = null;
    private OnAudioFocusChangeListener audioFocusListener;
    
	private Handler handler = new Handler();
	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	public ControllerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ControllerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "serviceReceiver received action "+action);
            int state = intent.getIntExtra("state", 0);
            if( client == null || !client.isConnected()) {
                Log.w(TAG, "Intent received, but jabber client is not present, skipping.");
                return;
            }

            // skip all headsets events if there is a call
            TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager != null && telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
                return;

            /*
             * Remote / headset control events
             */
            if (action.equalsIgnoreCase(ACTION_REMOTE_PLAYPAUSE)) {
            	playButtonPressed();
            } else if (action.equalsIgnoreCase(ACTION_REMOTE_PLAY)) {
//                client.resumePlaying();
            	client.resume();
            } else if (action.equalsIgnoreCase(ACTION_REMOTE_PAUSE)) {
//            	client.stopPlaying();
            	client.pause();
            } else if (action.equalsIgnoreCase(ACTION_REMOTE_BACKWARD)) {
            	playPrevious();
            } else if (action.equalsIgnoreCase(ACTION_REMOTE_STOP)) {
//            	client.stopPlaying();
//            	client.getCommandExecutor().stop();
            	client.pause();
            } else if (action.equalsIgnoreCase(ACTION_REMOTE_FORWARD)) {
                playNext();
            } else if (action.equalsIgnoreCase(ACTION_NETWORK_DISCONNECTED)) {
            	setNetworkReachable(false);
            } else if (action.equalsIgnoreCase(ACTION_NETWORK_CONNECTED)) {
            	setNetworkReachable(true);
//            } else if (action.equalsIgnoreCase(ACTION_REMOTE_LAST_PLAYLIST)) {
//                loadLastPlaylist();
//            } else if (action.equalsIgnoreCase(ACTION_WIDGET_INIT)) {
//                updateWidget(context);
            }

            /*
             * headset plug events
             */
//            if (mDetectHeadset) {
//                if (action.equalsIgnoreCase(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
//                    Log.i(TAG, "Headset Removed.");
//                    if (mLibVLC.isPlaying() && mCurrentMedia != null)
//                        pause();
//                }
//                else if (action.equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG) && state != 0) {
//                    Log.i(TAG, "Headset Inserted.");
//                    if (!mLibVLC.isPlaying() && mCurrentMedia != null)
//                        play();
//                }
//            }

            /*
             * Sleep
             */
//            if (action.equalsIgnoreCase(VLCApplication.SLEEP_INTENT)) {
//                stop();
//            }
        }
    };

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		// create jabber client
		client=new JabberClient(this);
		client.initiateClient(); 
        Log.d(TAG, "client:"+client);
        mRemoteControlClientReceiverComponent = new ComponentName(this,
                RemoteControlClientReceiver.class);
        Log.d(TAG, "mRemoteControlClientReceiverComponent:"+mRemoteControlClientReceiverComponent);
        
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetworkPlayer");
		WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NetworkPlayer");

        BusProvider.getInstance().register(this);
        getAppContext().setService(this);
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(ACTION_REMOTE_BACKWARD);
        filter.addAction(ACTION_REMOTE_PLAYPAUSE);
        filter.addAction(ACTION_REMOTE_PLAY);
        filter.addAction(ACTION_REMOTE_PAUSE);
        filter.addAction(ACTION_REMOTE_STOP);
        filter.addAction(ACTION_REMOTE_FORWARD);
//        filter.addAction(ACTION_REMOTE_LAST_PLAYLIST);
//        filter.addAction(ACTION_WIDGET_INIT);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
//        filter.addAction(VLCApplication.SLEEP_INTENT);
        registerReceiver(serviceReceiver, filter);
        
        if(!JabberUtils.isFroyoOrLater()) {
            /* Backward compatibility for API 7 */
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_BUTTON);
            mRemoteControlClientReceiver = new RemoteControlClientReceiver();
            registerReceiver(mRemoteControlClientReceiver, filter);
        }
//        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//        String dbChecksum = settings.getString("dbChecksum", null);
//        client.verifyCurrentDBChecksum(dbChecksum);
        
		tryAutoLogin();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart");
//		if (intent!=null&&intent.getFlags()==FLAG_RECEIVER_RINGING) {
//			Log.d(TAG, "received ringing command");
//			stopPlaying();
//		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
//		client.stopPlaying();
		if (mWakeLock != null) {
		    if (mWakeLock.isHeld()) mWakeLock.release();
		    mWakeLock = null;
		}
		if (wifiLock != null) {
			if (wifiLock.isHeld()) wifiLock.release();
			wifiLock = null;
		}
        unregisterReceiver(serviceReceiver);
        if (mRemoteControlClientReceiver != null) {
            unregisterReceiver(mRemoteControlClientReceiver);
            mRemoteControlClientReceiver = null;
        }
        BusProvider.getInstance().unregister(this);
	}
    protected ControllerApplication getAppContext(){
    	return (ControllerApplication)this.getApplicationContext();
    }
	
    // connection operations
	public void tryAutoLogin(){
		//get config
		final Resources res = getResources();
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    boolean autoLogin = settings.getBoolean(res.getString(R.string.pref_auto_login_key), false);
	    Log.d(TAG, "autoLogin:"+autoLogin);
	    if (autoLogin) {
	    	final String username = settings.getString(res.getString(R.string.pref_username_key), null);
	    	Log.d(TAG, "username:"+username);
//	    	Log.d(TAG, "password:"+password);
	    	if (username!=null) {
	    		try {
					AccountManager accountManager = AccountManager.get(ControllerService.this.getApplicationContext());
					Account userAccount = null;
					for (Account account : accountManager.getAccountsByType("com.google")) {
						if (account.name.equals(username)) {
							userAccount = account;
							break;
						}
					}
					accountManager.getAuthToken(userAccount, GetTokenTask.SCOPE, null, null, new OnTokenAcquired(username), handler);
	    		}catch (Exception e){
	    			Log.e(TAG, "Auto login failed, do manually login", e);
	    			e.printStackTrace();
	    		}
	    				
	    	}
	    }

	}

	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
		private String username;
		OnTokenAcquired(String username){
			this.username=username;
		}
		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			try {
				Bundle bundle = result.getResult();
 
				Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
				if (launch != null) {
//					startActivityForResult(launch, AUTHORIZATION_CODE);
					// the app doesn't have authorization, go to login fragment to do manual login
				} else {
					String token = bundle
							.getString(AccountManager.KEY_AUTHTOKEN);
 
					connect(username, token, AuthType.OAUTH);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public boolean connect(String username,String password, AuthType type){
		Log.d(TAG, "jabber client isConnected? "+isConnected());
        client.clearCurrentState();
		if (!isConnected()){
			client.getHubManager().clear();
			//check server connetivity
//			try {
//				Socket socket=new Socket(SERVER_NAME, SERVER_PORT);
//				socket.close();
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				Log.w(TAG, SERVER_NAME+" cannot be resolved.");
//				return false;
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				Log.w(TAG, "Cannot connect to "+SERVER_NAME+":"+SERVER_PORT);
//				return false;
//			}
			client.setCredential(username,password,type.getValue());
	        Thread t=new Thread(client);
	        t.start();
		}
        return true;
	}
	public boolean isConnected(){
		return client.isConnected();
	}

	public boolean isConnecting(){
		return client.getClientState()==ClientState.CONNECTING.ordinal();
	}
	public void logout(){
		Log.d(TAG, "clean then logout");
		client.cleanBeforeLogout();
		client.logout();
	}
	public void setNetworkReachable(boolean networkReachable){
        if (!networkReachable) {
	        if (client.getClientState()==ClientState.ONLINE.ordinal()) {
	        	client.cleanBeforeLogout();
	        	client.logout();
	        }
        }else{
            if (client.getClientState()==ClientState.OFFLINE.ordinal())
                tryAutoLogin();
        }
    }
	
	public boolean isHubbed(){
		return client.isHubbed();
	}

    

//	public void showPlayStateNotification(){
//	notificationHelper.showNotification(this);
//}
//
//public void hidePlayStateNotification(){
//	notificationHelper.hideNotification(this);
//}
	private void showPlayStateNotification() {
	    try {
	    	Bitmap largeIcon=null;
	        if (getCurrentSongItem().getPreviewPath() != null){
	        	File imageFile=new File(getCurrentSongItem().getPreviewPath());
	    		Log.d(TAG, "preview file exists?"+imageFile.exists());
	    		if(imageFile.exists()){
	    			Log.d(TAG, "preview file size:"+imageFile.length()+" "+imageFile.getAbsolutePath());
	    			largeIcon = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
	    		}
	        }else{
	        	Drawable myDrawable = getResources().getDrawable(R.drawable.music_note);
	        	largeIcon = ((BitmapDrawable) myDrawable).getBitmap();        			
	    	}
	        // add notification to status bar
	        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
	        .setSmallIcon(R.drawable.music_note)
	        .setLargeIcon(largeIcon)
	        .setContentTitle(getCurrentSongItem().getTitle())
	        .setTicker(getCurrentSongItem().getTitle() + " - " + getCurrentSongItem().getArtist())
	        .setContentText(getCurrentSongItem().getArtist())
	        .setContentInfo(getCurrentSongItem().getAlbum())
	        .setAutoCancel(false)
	        .setOngoing(true);
	
	        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
	        notificationIntent.setAction(Intent.ACTION_MAIN);
	        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	//        notificationIntent.putExtra(START_FROM_NOTIFICATION, true);
	        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	
	        builder.setContentIntent(pendingIntent);
	        startForeground(3, builder.build());
	    }
	    catch (NoSuchMethodError e){
	        // Compat library is wrong on 3.2
	        // http://code.google.com/p/android/issues/detail?id=36359
	        // http://code.google.com/p/android/issues/detail?id=36502
	    }
	}
	
	private void hidePlayStateNotification() {
	    stopForeground(true);
	}
	
	/**
	 * Set up the remote control and tell the system we want to be the default receiver for the MEDIA buttons
	 * @see http://android-developers.blogspot.fr/2010/06/allowing-applications-to-play-nicer.html
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setUpRemoteControlClient() {
	//    Context context = VLCApplication.getAppContext();
	    AudioManager audioManager = (AudioManager)this.getAppContext().getSystemService(AUDIO_SERVICE);
	//    audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
	//            AudioManager.AUDIOFOCUS_GAIN);
	    if(JabberUtils.isICSOrLater()) {
	    	Log.d(TAG, "setUpRemoteControlClient");
	        audioManager.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
	
	        if (mRemoteControlClient == null) {
	            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
	            mediaButtonIntent.setComponent(mRemoteControlClientReceiverComponent);
	            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
	
	            // create and register the remote control client
	            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
	            audioManager.registerRemoteControlClient(mRemoteControlClient);
	        }
	
	        mRemoteControlClient.setTransportControlFlags(
	                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
	                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
	                RemoteControlClient.FLAG_KEY_MEDIA_NEXT );
	    } else if (JabberUtils.isFroyoOrLater()) {
	        audioManager.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
	    }
	}

	/**
	 * A function to control the Remote Control Client. It is needed for
	 * compatibility with devices below Ice Cream Sandwich (4.0).
	 *
	 * @param p Playback state
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setRemoteControlClientPlaybackState(NetworkPlayerState state) {
	    if(!JabberUtils.isICSOrLater() || mRemoteControlClient == null)
	        return;
	    Log.d(TAG, "setRemoteControlClientPlaybackState:"+state);
	    if (state==NetworkPlayerState.PLAYING) {
	    	mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
	    }else if (state==NetworkPlayerState.IDLE) {
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
	    }
	}
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void updateRemoteControlClientMetadata() {
	    if(!JabberUtils.isICSOrLater()) // NOP check
	        return;
	    Log.d(TAG, "updateRemoteControlClientMetadata");
	    if (mRemoteControlClient != null) {
	        Log.d(TAG, "updateRemoteControlClientMetadata mRemoteControlClient:"+mRemoteControlClient);
	        MetadataEditor editor = mRemoteControlClient.editMetadata(true);
	        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getCurrentSongItem().getAlbum());
	        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getCurrentSongItem().getArtist());
	//        editor.putString(MediaMetadataRetriever.METADATA_KEY_GENRE, getCurrentSongItem().getGenre());
	        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getCurrentSongItem().getTitle());
	        editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, getCurrentSongItem().getLength());
	        if (getCurrentSongItem().getPreviewPath()!=null&&getCurrentSongItem().getPreviewPath().length()>0) {
	    		File imageFile=new File(getCurrentSongItem().getPreviewPath());
	    		Log.d(TAG, "RemoteControlClient preview file exists?"+imageFile.exists());
	    		if(imageFile.exists()){
	    			Log.d(TAG, "RemoteControlClient preview file size:"+imageFile.length()+" "+imageFile.getAbsolutePath());
	    		    Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
	                editor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, myBitmap);
	    		}
	        }
	        editor.apply();
	    }
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private void changeAudioFocus(boolean gain) {
	    if(!JabberUtils.isFroyoOrLater()) // NOP if not supported
	        return;
	
	    if (audioFocusListener==null) audioFocusListener = new OnAudioFocusChangeListener() {
	        @Override
	        public void onAudioFocusChange(int focusChange) {
	        	Log.d(TAG, "onAudioFocusChange:"+focusChange);
	            if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ||
	               focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
	                /*
	                 * Lower the volume to 36% to "duck" when an alert or something
	                 * needs to be played.
	                 */
	//                LibVLC.getExistingInstance().setVolume(36);
	            	Log.d(TAG, "OnAudioFocusChangeListener:low vol");
	            } else {
	//                LibVLC.getExistingInstance().setVolume(100);
	            	Log.d(TAG, "OnAudioFocusChangeListener:high vol");
	            }
	        }
	    };
	
	    AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
	    if(gain) {
	        int i=am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	        Log.d(TAG, "requestAudioFocus:"+i);
	    }else
	        am.abandonAudioFocus(audioFocusListener);
	
	}
	

	// UI control
	public int getCurrentStreamingProgress() {
		Log.d(TAG, "getCurrentStreamingProgress, calling jabberclient.getCurrentMusicStreamingProgress()");
		return client.getCurrentMusicStreamingProgress();
	}
	public void refreshCurrentDirectory(){
		client.refreshCurrentDirectory();
	}
	public void browseMusicDirectory(DirectoryItem dir) {
		client.browseMusicDirectory(dir);
	}
    public void playSong(final SongItem songItem){
		if (playSongTask==null||playSongTask.getStatus()==AsyncTask.Status.FINISHED){
			playSongTask=new PlaySongTask(this, client);
			playSongTask.execute("play", songItem);
		} else {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
				}
			});
		}
//		handler.post(new Runnable(){
//			public void run() {
//		    	client.playSong(songItem);
//			}
//		});
    }

	public void playButtonPressed() {
		if (playSongTask==null||playSongTask.getStatus()==AsyncTask.Status.FINISHED){
			playSongTask=new PlaySongTask(this, client);
			playSongTask.execute("playpause");
		} else {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
				}
			});
		}
//		client.playPause();
	}

	public void clearCurrentPlayingSession() {
		// this should be done automatically
//		client.clearCurrentPlayingSession();
	}

	public ArrayList<DisplayItem> getCurrentList() {
		return client.currentList;
	}
	
	public DisplayItem getParent() {
		if (client.currentList.size()>0&&(client.currentList.get(0).getIcon()==R.drawable.ic_action_goleft)) {
			return client.currentList.get(0);
		}
		return null;
	}

	public SongItem getCurrentSongItem() {
		return client.currentSongItem;
	}

	public int getCurrentPlayerProgress() {
		return client.getCurrentPlayingProgress();
	}

	public int getCurrentPlayerState() {
		return client.getNetworkPlayerState();
	}
	
	public boolean isShuffle() {
		return client.shuffle;
	}

	public int getRepeatMode() {
		return client.repeatMode;
	}
	public String getCurrentDirTitle() {
		return client.getCurrentDirTitle();
	}

	public void forceLogout() {
		if (client.getClientState()==ClientState.ONLINE.ordinal()) {
			client.cleanBeforeLogout();
			client.logout();
		}
	}
	
	public void shuffle(){
		client.shuffle();
	}
	public void repeat() {
		client.repeat();
	}
    public void playNext(){
//		handler.post(new Runnable(){
//			public void run() {
//		        if (client.playNext()==-1) { 
//		        	BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
//		        }
//			}
//		});
		if (playSongTask==null||playSongTask.getStatus()==AsyncTask.Status.FINISHED){
			playSongTask=new PlaySongTask(this, client);
			playSongTask.execute("next");
		} else {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
				}
			});
		}
    }
    public void playPrevious(){
		if (playSongTask==null||playSongTask.getStatus()==AsyncTask.Status.FINISHED){
			playSongTask=new PlaySongTask(this, client);
			playSongTask.execute("previous");
		} else {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
				}
			});
		}
//		handler.post(new Runnable(){
//			public void run() {
//		        if (client.playPrevious()==-1) { 
//		        	BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
//		        }
//			}
//		});
    }

    public void seekingStart(){
		client.seekingStart();
    }
    public void seekingStop(int currentProgress){
		if (playSongTask==null||playSongTask.getStatus()==AsyncTask.Status.FINISHED){
			playSongTask=new PlaySongTask(this, client);
			playSongTask.execute("seek", new Integer(currentProgress));
//			client.seekingStop(currentProgress);
		} else {
			handler.post(new Runnable(){
				public void run() {
					BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
				}
			});
		}
    }

	// callback operations
	@Subscribe public void onPlayerStateChanged(PlayerStateChangedEvent event) {
		Log.d(TAG, "onPlayerStateChanged:"+event.state);
		if (event.state==NetworkPlayerState.PLAYING.ordinal()) {
			changeAudioFocus(true);
			setUpRemoteControlClient();
			updateRemoteControlClientMetadata();
			setRemoteControlClientPlaybackState(NetworkPlayerState.PLAYING);
			if (!mWakeLock.isHeld()) mWakeLock.acquire();
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (mWifi.isConnected()&&!wifiLock.isHeld()) wifiLock.acquire();
			showPlayStateNotification();
		}
		if (event.state==NetworkPlayerState.IDLE.ordinal()) { //currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_PAUSED||
			if (mWakeLock != null&&mWakeLock.isHeld()) {
			    mWakeLock.release();
			}
			if (wifiLock != null&&wifiLock.isHeld()) {
				wifiLock.release();
			}
			setRemoteControlClientPlaybackState(NetworkPlayerState.IDLE);
			changeAudioFocus(false);
			hidePlayStateNotification();
		}
//		updateWidget(this);
//		getAppContext().renderButtonsInUI(currentPlayerState);
	}
	
	@Subscribe public void showToastMessage(ToastMessageEvent event) {
		if (toast==null) {
			toast=Toast.makeText(ControllerService.this, event.getMessage(),Toast.LENGTH_SHORT);
			toast.show();
		} else if (!toast.getView().isShown()) {
			toast.setText(event.getMessage());
			toast.show();
		}
//		Toast.makeText(ControllerService.this, event.getMessage(),
//                Toast.LENGTH_SHORT).show();
	}

}
