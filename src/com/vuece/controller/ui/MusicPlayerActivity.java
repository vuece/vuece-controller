package com.vuece.controller.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.squareup.otto.Subscribe;
import com.vuece.controller.R;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.BrowsingStartedEvent;
import com.vuece.controller.event.ClientStateChangedEvent;
import com.vuece.controller.event.HubExitedEvent;
import com.vuece.controller.event.HubOpenedEvent;
import com.vuece.controller.event.PlayerStateChangedEvent;
import com.vuece.controller.event.SongListChangedEvent;
import com.vuece.controller.model.AuthType;
import com.vuece.controller.model.ClientEvent;
import com.vuece.controller.model.NetworkPlayerEvent;
import com.vuece.controller.model.NetworkPlayerState;
import com.vuece.controller.model.ParentDirectoryItem;
import com.vuece.controller.service.ControllerService;
import com.vuece.controller.service.ControllerService.LocalBinder;
import com.vuece.vtalk.android.jni.JabberClient;
import com.vuece.vtalk.android.util.Log;

public class MusicPlayerActivity extends SherlockFragmentActivity 
		implements LoginFragment.LoginListener, SongListFragment.OnSongSelectedListener, ToolbarFragment.ToolbarListener, 
		SongDetailFragment.SongDetailListener, HubListFragment.OnHubSelectedListener{ 
	private static String TAG = "vuece/MusicPlayerActivity";
    ControllerService mService;
    boolean mBound = false;
	private ViewFlipper viewFlipper;
	private Menu menu;
    
	private SongAdapter songAdapter;
	private SongListFragment listFragment;
	private SongDetailFragment songFragment;
	private ToolbarFragment toolbarFragment;
	private LoginFragment loginFragment;
	private ParentDirectoryItem parent;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate.");

        // add shortcut to home screen
        createShortcut();
        
        setContentView(R.layout.main);
        final ActionBar ab = getSupportActionBar();

        // set defaults for logo & home up
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayUseLogoEnabled(false);
        ab.setDisplayShowTitleEnabled(true);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        // Check whether the activity is using the layout version with
        // the fragment_container FrameLayout. If so, we must add the first fragment
        if (findViewById(R.id.fragment_container_main) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
//            if (savedInstanceState != null) {
//                return;
//            }
            Log.d(TAG, "show login view");
            // Create an instance of ExampleFragment
            if (loginFragment==null) loginFragment = new LoginFragment();
//            loginFragment.setService(mService);

            // In case this activity was started with special instructions from an Intent,
            // pass the Intent's extras to the fragment as arguments
//            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
    		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
    		Log.d(TAG, "on create fragment:"+fragment);
    		if (fragment==null)
	    		getSupportFragmentManager().beginTransaction()
	                    .add(R.id.fragment_container_main, loginFragment).commit();
        }
        if (findViewById(R.id.fragment_container_song) != null) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
        }
    	toolbarFragment = (ToolbarFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_toolbar);
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	//        toolbarFragment.setService(mService);
    }
    
    private void createShortcut() {
    	
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    boolean shortcutInstalled = settings.getBoolean("shortcut_installed", false);
	    Log.d(TAG, "shortcut installed? "+shortcutInstalled);
	    if (shortcutInstalled) return;
    	
        Intent homeScreenShortCut= new Intent(this,
        		MusicPlayerActivity.class);
        homeScreenShortCut.setAction(Intent.ACTION_MAIN);
        homeScreenShortCut.putExtra("duplicate", false);
        homeScreenShortCut.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        homeScreenShortCut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //shortcutIntent is added with addIntent
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, homeScreenShortCut);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Vuece Music");
        addIntent.putExtra("duplicate", false);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); 
        getApplicationContext().sendBroadcast(addIntent);
        
		SharedPreferences.Editor editor = settings.edit();
	    editor.putBoolean("shortcut_installed", true);
	    editor.commit();
    }
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mService!=null)
        	NotificationHelper.createInstance().hideNotification(mService);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        Log.d(TAG, "onPause");
        BusProvider.getInstance().unregister(this);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        Log.d(TAG, "onResume");
        BusProvider.getInstance().register(this);
        if (mService!=null&&!mService.isConnected()) {
    		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
    		if (fragment!=null&&!(fragment instanceof LoginFragment)) {
//    			LoginFragment loginFragment = new LoginFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_main, loginFragment).commit();
                if (findViewById(R.id.fragment_container_song) == null) {
        			View toolbarView=(View) findViewById(R.id.fragment_container_toolbar);
        			toolbarView.setVisibility(View.GONE);
                }
    		}
    		
        }else if (mService!=null&&mService.isConnected()){
            if (mService.isHubbed()) {
//        		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
//        		if (fragment instanceof HubListFragment)
//        			showList();
            }else {
        		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
        		if (!(fragment instanceof HubListFragment))
        			showHubList();
                if (findViewById(R.id.fragment_container_song) == null) {
	    			View toolbarView=(View) findViewById(R.id.fragment_container_toolbar);
	    			toolbarView.setVisibility(View.GONE);
                }
            }
        }
	}
	
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Bind to LocalService
        Intent intent = new Intent(this, ControllerService.class);
        if (!mBound) {
        	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        	mBound=true;
        }
        Log.d(TAG, "this.getAppContext().isConnected()?"+(mService!=null&&mService.isConnected()));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the currently selected menu XML resource.
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.music_player, menu);
//            menu.findItem(R.id.roster_menu_go_root).setIcon(android.R.drawable.ic_menu_share);
//            menu.findItem(R.id.roster_menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
//            menu.findItem(R.id.roster_menu_exit).setIcon(R.drawable.menu_icon_exit);
            this.menu=menu;
          final MenuItem songMenuItem = (MenuItem) menu.findItem(R.id.menu_song);
          if (findViewById(R.id.fragment_container_song) != null) {
        	  songMenuItem.setEnabled(false);
        	  songMenuItem.setVisible(false);
          }else{
        	  songMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        		  public boolean onMenuItemClick(MenuItem item) {
                  	if (mService.isHubbed()&&songFragment!=null) {
    	    			showSong();
                	}
                  	return false;
        		  }
        	  });
          }
//            final MenuItem switchItem = (MenuItem) menu.findItem(R.id.menu_switch);
//            if (findViewById(R.id.fragment_container_song) != null) {
//            	switchItem.setEnabled(false);
//            	switchItem.setVisible(false);
//            }else{
//                switchItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//                    // on selecting show progress spinner for 1s
//                    public boolean onMenuItemClick(MenuItem item) {
//                    	if (mService.getCurrentSongItem()==null) return false;
//                    	onSwitchButtonPressed();
////                    	int currentViewIndex=viewFlipper.indexOfChild(viewFlipper.getCurrentView());
////                    	if (currentViewIndex==0) {
////                    		viewFlipper.setDisplayedChild(1);
////                    	}else if (currentViewIndex==1){
////                    		viewFlipper.setDisplayedChild(0);
////                    	}
//                    	return false;
//                    }
//                });
//            }

            return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
    	FragmentTransaction transaction;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent lIntent1 = new Intent(this, PreferencesActivity.class);
                lIntent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(lIntent1);
                break;
            case R.id.menu_exit:
            {
            	exitApp();
                    //finish();
            }
                    break;
            case R.id.menu_go_root:
                if (mService.isHubbed()) {
                	if (!(fragment instanceof SongListFragment)) showList();
                	mService.browseMusicDirectory(null);
                }
                break;
//            case R.id.menu_song:
//            	if (mService.isHubbed()&&songFragment!=null) {
//	    			showSong();
//            	}
//            	break;
            case R.id.menu_list:
            	if (mService.isHubbed()) {
            		showList();
            	}
            	break;
            case R.id.menu_hub_list:
            	if (mService!=null&&mService.isConnected()) showHubList();
            	break;
            case android.R.id.home:
            	mService.browseMusicDirectory(parent);
            default:
            	return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (viewFlipper!=null) {
		    int position = viewFlipper.getDisplayedChild();
		    savedInstanceState.putInt("TAB_NUMBER", position);
		    Log.d(TAG, "saved tab number:"+position);
		}
	}
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
        	LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(TAG, "mBound:"+mBound);
//            populateService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d(TAG, "mBound:"+mBound);
        }
    };
    
//    private void populateService(){
//    	Log.d(TAG, "populate service to fragments");
//    	if (toolbarFragment!=null) toolbarFragment.setService(mService);
//    	if (songFragment!=null) songFragment.setService(mService);
//    }
    
	@Override
	public void onBackPressed() {
		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
		Log.d(TAG, "current fragment:"+fragment.getClass());
		if (fragment instanceof SongDetailFragment) {
			Log.d(TAG, "showing song view");
			mService.clearCurrentPlayingSession();
//			SongListFragment newFragment= new SongListFragment();
//			SongAdapter songAdapter=new SongAdapter(this, R.layout.media_item, mService.getCurrentList());
//			((SongListFragment)newFragment).setSongAdapter(songAdapter);
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_container_main, listFragment);
			transaction.commit();	
		}else if (fragment instanceof SongListFragment || findViewById(R.id.fragment_container_song) != null) {
			Log.d(TAG, "showing list view");
			new AlertDialog.Builder(this)
			    .setTitle("Exit Vuece Music")
			    .setMessage("Are you sure you want to exit?")
			    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
	                    Handler handler=new Handler();
	                    handler.post(new Runnable() {
				            public void run() {
				            	mService.clearCurrentPlayingSession();
				            	mService.logout();
				            }
				        });
			        }
			     })
			    .setNegativeButton("No", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			            // do nothing
			        }
			     })
			     .show();
		} else if (fragment instanceof LoginFragment) {
			onCancelButtonClicked();
		} else if (fragment instanceof HubListFragment) {
			Log.d(TAG, "showing hub list view");
			new AlertDialog.Builder(this)
			    .setTitle("Exit Vuece Music")
			    .setMessage("Are you sure you want to exit?")
			    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
						exitApp();
			        }
			     })
			    .setNegativeButton("No", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			            // do nothing
			        }
			     })
			     .show();
		}
		//super.onBackPressed();
	}
	
	private void exitApp() {
        if (!mService.isConnected()) {
        	finish();
        	return;
        }
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
            	mService.clearCurrentPlayingSession();
            	mService.logout();
            }
        }, 100);
	}
	
	public void showHubList(){
		getSupportActionBar().setTitle("Hubs");
		Fragment hubListFragment = new HubListFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack
		transaction.replace(R.id.fragment_container_main, hubListFragment);
//		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();	
	}
	@Subscribe public void onHubOpened(HubOpenedEvent event){
//		currentView=2;
		//if (menu!=null) menu.getItem(0).setEnabled(true);
		showList();
		getToolbarFragment().clearTitles();
	}
	private void showList(){
		if (listFragment==null) {
			listFragment = new SongListFragment();
			songAdapter=new SongAdapter(this, R.layout.media_item, mService.getCurrentList());
			listFragment.setSongAdapter(songAdapter);
		}
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//			transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
//		else
		transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
		
		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack
		transaction.replace(R.id.fragment_container_main, listFragment);
//		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();	
	}
	@Subscribe public void onHubExited(HubExitedEvent event){
		showHubList();
		View toolbarView=(View) findViewById(R.id.fragment_container_toolbar);
		toolbarView.setVisibility(View.GONE);
		menu.findItem(R.id.menu_list).setActionView(null);
	}
	@Subscribe public void onClientStateChanged(ClientStateChangedEvent event) {
		Log.d(TAG, "client state:"+event.state);
		if (event.event==ClientEvent.LOGOUT_OK.getValue()) {
		    Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
		    finish();
		}else if (event.event==ClientEvent.LOGIN_OK.getValue()) {
			showHubList();
		}else if (event.event==ClientEvent.LOGGING_IN.getValue()) {
//			Handler connectionTimeoutMonitor=new Handler();
//            connectionTimeoutMonitor.postDelayed(new Runnable() {
//            	public void run() {
//            		if (mService.isConnecting()) {
//            			Log.i(TAG, "force logout due to timeout.");
//            			mService.forceLogout();
//	            		Toast.makeText(MusicPlayerActivity.this, R.string.time_out,
//	                            Toast.LENGTH_SHORT).show();
//            		}
//            	}
//            }, 20*1000);
		}
	}
	@Subscribe public void onBrowsingStarted(BrowsingStartedEvent event){
		menu.findItem(R.id.menu_list).setActionView(R.layout.indeterminate_progress_action);
	}
	@Subscribe public void onSongListChanged(SongListChangedEvent event){
		getSupportActionBar().setTitle(mService.getCurrentDirTitle());
		menu.findItem(R.id.menu_list).setActionView(null);
		parent=(ParentDirectoryItem) mService.getParent();
		Log.d(TAG, "parent:"+parent);
		if (parent!=null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}else
			getSupportActionBar().setDisplayHomeAsUpEnabled(false);

	}
	@Subscribe public void onPlayerStateChanged(PlayerStateChangedEvent event){
		if (event.event==NetworkPlayerEvent.NETWORK_ERR.getValue()) {
			showAlert("Network Error! Please check your network settings.");
		} else if (event.event==NetworkPlayerEvent.TIMEOUT.getValue()) {
			showAlert("Something goes wrong... Please try again later.");
		}
		if (event.state==NetworkPlayerState.PLAYING.ordinal()){
			if (getSongDetailFragment()!=null) getSongDetailFragment().endWaiting();
			renderSongDetail();
//			showSong();
		}else if (event.state==NetworkPlayerState.WAITING.ordinal()){
			if (getSongDetailFragment()!=null) getSongDetailFragment().startWaiting();
		}else if (event.state==NetworkPlayerState.IDLE.ordinal()){
			if (getSongDetailFragment()!=null) getSongDetailFragment().endWaiting();
		}
		if (getToolbarFragment()!=null) {
			renderButtons(event.state);
			renderShuffleImage();
			renderRepeatImage();
		}
	}
	
	private void showAlert(String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("ERROR");
		alertDialog.setMessage(message);
		alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alertDialog.setIcon(R.drawable.ic_launcher);
		alertDialog.show();
	}

	public void showSong() {
		
		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
		Log.d(TAG, "current fragment:"+fragment.getClass());
		if (fragment instanceof SongDetailFragment) return;
		
		Log.d(TAG, "showSong - Start");
		// if this is a tablet, which shows SongDetailFragment all the time
		if (findViewById(R.id.fragment_container_song) != null) {
			Log.d(TAG, "showSong - 1");
			songFragment = this.getSongDetailFragment();
			Log.d(TAG, "showSong - 2");
		}else{
			// this is a mobile, which doesn't show SongDetailFragment all the time
			//if (JabberClient.getInstance().currentSongItem==null) return;
			if (songFragment==null) songFragment = new SongDetailFragment();
			
			View toolbarView=(View) findViewById(R.id.fragment_container_toolbar);
			toolbarView.setVisibility(View.VISIBLE);
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_container_main, songFragment);
			transaction.commit();	
		}
	}
	
	@Override
	public void onSongSelected(int position) {
		// TODO Auto-generated method stub
		
	}
	
	// Login fragment callbacks
	@Override
	public void onLoginButtonClicked(String username, String password, AuthType type) {
        mService.connect(username, password, type);
	}
	@Override
	public void onCancelButtonClicked() {
    	mService.forceLogout();
    	finish();
	}
	
	// Toolbar fragment callbacks
//	@Override
//	public void onSwitchButtonPressed() {
//		Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
//		Log.d(TAG, "current fragment:"+fragment.getClass());
//		Fragment newFragment=null;
//		if (fragment instanceof SongDetailFragment){
////			newFragment= new SongListFragment();
////			SongAdapter songAdapter=new SongAdapter(this, R.layout.media_item, mService.getCurrentList());
////			((SongListFragment)newFragment).setSongAdapter(songAdapter);
//			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//			transaction.replace(R.id.fragment_container_main, listFragment);
//			transaction.commit();	
//			menu.findItem(R.id.menu_switch).setIcon(R.drawable.ic_action_music_1);
//		}else if (fragment instanceof SongListFragment){
////			newFragment=new SongDetailFragment();
////			((SongDetailFragment)newFragment).setService(mService);
//			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//			transaction.replace(R.id.fragment_container_main, songFragment);
//			transaction.commit();	
//			menu.findItem(R.id.menu_switch).setIcon(R.drawable.ic_action_list);
//		}
//	}
	@Override
	public void onRepeatButtonPressed() {
    	mService.repeat();
    	renderRepeatImage();
	}
	@Override
	public void onShuffleButtonPressed() {
    	mService.shuffle();
    	renderShuffleImage();
	}
	@Override
	public void onPlayButtonPressed() {
    	mService.playButtonPressed();
	}
	@Override
	public void onStopButtonPressed() {
		mService.clearCurrentPlayingSession();
		if (getSongDetailFragment()==null) return;
		getSongDetailFragment().clearSongView();
	}
	@Override
	public void onPreviousButtonPressed() {
    	mService.playPrevious();
	}
	@Override
	public void onNextButtonPressed() {
    	mService.playNext();
	}
	@Override
	public void onToolbarInitiate() {
		if (mService!=null&&mService.isConnected()&&mService.isHubbed()) {
        	renderButtons(mService.getCurrentPlayerState());
        	renderShuffleImage();
        	renderRepeatImage();
        }
	}
	
	// SongDetail fragment callbacks
	@Override
	public void onSongDetailInitiate() {
		if (mService!=null&&mService.isConnected()) 
			renderSongDetail();
	}
	@Override
	public void onStartTrackingTouch() {
		mService.seekingStart();
	}
	@Override
	public void onStopTrackingTouch(int currentProgress) {
		mService.seekingStop(currentProgress);
	}

	// HubList dialog fragment callbacks
	@Override
	public void onHubSelected(final String jid) {
		if (jid.equalsIgnoreCase(JabberClient.getInstance().currentHubJid)) {
			showList();
			return;
		}
		if (JabberClient.getInstance().getNetworkPlayerState()==NetworkPlayerState.PLAYING.ordinal()) {
			new AlertDialog.Builder(this)
		    .setTitle("Browse hub")
		    .setMessage("Are you sure you want to browse a different hub? This will stop the current playing music.")
		    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	JabberClient.getInstance().enterHub(jid);
		        }
		     })
		    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	dialog.dismiss();
		        }
		     })
		    .setIcon(android.R.drawable.ic_dialog_alert)
		    .show();
		}else
			JabberClient.getInstance().enterHub(jid);
//		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//		transaction.remove(songFragment).commit();
//		songFragment=null;
	}

	// private functions that communicate with Fragments
	private ToolbarFragment getToolbarFragment(){
		return (ToolbarFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_toolbar);
	}
	private SongDetailFragment getSongDetailFragment(){
		
		Log.d(TAG, "getSongDetailFragment - 1");
		
		if (findViewById(R.id.fragment_container_song) != null){
			Log.d(TAG, "getSongDetailFragment - 2");
			return (SongDetailFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_songdetail);
		}
		else {
			Log.d(TAG, "getSongDetailFragment - 3");
			if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_main) instanceof SongDetailFragment)
			{
				Log.d(TAG, "getSongDetailFragment - 4");
				return (SongDetailFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container_main);
			}
				
		}
		return null;
	}
	private void renderShuffleImage(){
		if (mService.isShuffle()){
			getToolbarFragment().showShuffleOn();
		}else{
			getToolbarFragment().showShuffleOff();
		}
	}
	private void renderRepeatImage(){
		if (mService.getRepeatMode()==0){
			getToolbarFragment().showRepeatOff();
		}else if (mService.getRepeatMode()==1){
			getToolbarFragment().showRepeatOnce();
		}else if (mService.getRepeatMode()==2){
			getToolbarFragment().showRepeatOn();
		}
	}
	private void renderButtons(int currentPlayerState){
		if (currentPlayerState==NetworkPlayerState.PLAYING.ordinal()){
			getToolbarFragment().showPauseImage();
			getToolbarFragment().showTitles(mService.getCurrentSongItem().getTitle(),mService.getCurrentSongItem().getArtist());
		}else if (currentPlayerState==NetworkPlayerState.WAITING.ordinal()){
			getToolbarFragment().showPauseImage();
		}else if (currentPlayerState==NetworkPlayerState.IDLE.ordinal()){
			getToolbarFragment().showPlayImage();
		}
	}
	private void renderSongDetail(){
		
		Log.d(TAG, "renderSongDetail() - Start 1");
		Log.d(TAG, "renderSongDetail():"+getSongDetailFragment());
		
		if (getSongDetailFragment()==null||mService.getCurrentSongItem()==null) return;
		
		Log.d(TAG, "renderSongDetail() - 1");
		
		getSongDetailFragment().showDetails(mService.getCurrentSongItem().getAlbum(), 
				mService.getCurrentSongItem().getArtist().length()>0?mService.getCurrentSongItem().getArtist():"Unknown Artist",
				mService.getCurrentSongItem().getTitle(),
				mService.getCurrentSongItem().getLength());
		
		Log.d(TAG, "renderSongDetail() - 2");
		
		getSongDetailFragment().updateCurrentPlayerProgress(mService.getCurrentPlayerProgress());
		
		Log.d(TAG, "renderSongDetail() - 3");
		
		getSongDetailFragment().updateCurrentStreamingProgress(mService.getCurrentStreamingProgress());
		
		Log.d(TAG, "renderSongDetail() - 4");
		
		if (mService.getCurrentSongItem().getPreviewPath()!=null) {
			Log.d(TAG, "renderSongDetail() - 5");
			
			getSongDetailFragment().renderPreview(mService.getCurrentSongItem().getPreviewPath());
			
			Log.d(TAG, "renderSongDetail() - 6");
			
		}else{
			
			Log.d(TAG, "renderSongDetail() - 7");
			
			getSongDetailFragment().showDefaultPreview();
			
			Log.d(TAG, "renderSongDetail() - 8");
		}
	}
}