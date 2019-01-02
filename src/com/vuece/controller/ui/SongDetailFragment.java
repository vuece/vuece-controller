package com.vuece.controller.ui;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.vuece.controller.R;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.PlayerProgressEvent;
import com.vuece.controller.event.PreviewAvailableEvent;
import com.vuece.controller.event.PreviewNotAvailableEvent;
import com.vuece.controller.event.StreamingProgressEvent;
import com.vuece.vtalk.android.util.JabberUtils;
import com.vuece.vtalk.android.util.Log;

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

public class SongDetailFragment extends Fragment {
	SongDetailListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface SongDetailListener {
        public void onSongDetailInitiate();
        public void onStartTrackingTouch();
        public void onStopTrackingTouch(int currentProgress);
    }

    private static String TAG = "SongDetailFragment";

	private TextView albumName;
	private TextView artistName;
	private TextView trackName;
	private TextView currentTime;
	private TextView totalTime;
	private ImageView albumImage;
	private SeekBar progressBar;
	private boolean userSeeking;
	private ProgressBar waitingBar;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
//            mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
        }
        

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.song, container, false);
    }

	@Override
	public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        
		albumName = (TextView)getActivity().findViewById(R.id.albumname);
		artistName = (TextView)getActivity().findViewById(R.id.artistname);
		trackName = (TextView)getActivity().findViewById(R.id.trackname);
		currentTime = (TextView)getActivity().findViewById(R.id.currenttime);
		totalTime = (TextView)getActivity().findViewById(R.id.totaltime);
        albumImage = (ImageView)getActivity().findViewById(R.id.albumImage);
        waitingBar = (ProgressBar) getActivity().findViewById(R.id.progressbar2);
        if (!(waitingBar.getIndeterminateDrawable() instanceof SmoothProgressDrawable))
        	waitingBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(this.getActivity()).color(0x9999cc03).interpolator(new AccelerateInterpolator()).build());
		progressBar=(SeekBar)getActivity().findViewById(R.id.progress);
		progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			private int currentProgress;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.d(TAG, "progress:"+progress+";fromUser:"+fromUser);
				if (fromUser) {
					currentProgress=progress;
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.d(TAG, "user started moving:"+currentProgress);
				currentProgress=0;
				mCallback.onStartTrackingTouch();
				userSeeking=true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mCallback.onStopTrackingTouch(currentProgress);
				userSeeking=false;
			}
			
		});
		
		
		
	}
	
	
	
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (SongDetailListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ToolbarListener");
        }
    }
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        Log.d(TAG, "onResume");
        BusProvider.getInstance().register(this);
        mCallback.onSongDetailInitiate();
	}
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        Log.d(TAG, "onPause");
        BusProvider.getInstance().unregister(this);
	}
    @Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
    
    //message subscription
	@Subscribe public void onPlayerProgress(PlayerProgressEvent event){
		if (progressBar!=null) {
			updateCurrentPlayerProgress(event.getProgress());
		}
	}
	@Subscribe public void onStreamingProgress(StreamingProgressEvent event){
		if (progressBar!=null) progressBar.setSecondaryProgress(event.getProgress());
	}

	@Subscribe public void onPreviewNotAvailable(PreviewNotAvailableEvent event){
		showDefaultPreview();
	}
	public void clearSongView() {
		albumImage.setImageDrawable(getResources().getDrawable(R.drawable.music_note));
		progressBar.setProgress(0);
		progressBar.setSecondaryProgress(0);
		albumName.setText("");
		trackName.setText("");
		artistName.setText("");
		totalTime.setText("");
		currentTime.setText("");
		userSeeking=false;
	}
	@Subscribe public void onPreviewAvailable(PreviewAvailableEvent event){
		Log.d(TAG, "preview path:"+event.getPath());
		if (event.getPath()!=null) {
			renderPreview(event.getPath());
		}
	}
	public void showDefaultPreview(){
	    albumImage.setImageDrawable(getResources().getDrawable(R.drawable.music_note));
	}
	public void renderPreview(String path){
		File imageFile=new File(path);
		Log.d(TAG, "preview file exists?"+imageFile.exists());
		if(imageFile.exists()){
			Log.d(TAG, "preview file size:"+imageFile.length()+" "+imageFile.getAbsolutePath());
		    Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		    albumImage.setImageBitmap(myBitmap);
		}
	}
	public void showDetails(String album, String artist, String track, int length){
		albumName.setText(album);
		artistName.setText(artist);
		trackName.setText(track);
		progressBar.setMax(length);
		totalTime.setText(JabberUtils.getTimeFormatShort(length));
	}
	public void updateCurrentPlayerProgress(int progress){
		currentTime.setText(JabberUtils.getTimeFormatShort(progress));
		if (!userSeeking) progressBar.setProgress(progress);
	}
	public void updateCurrentStreamingProgress(int progress){
		progressBar.setSecondaryProgress(progress);
	}
	public void startWaiting(){
		progressBar.setVisibility(View.GONE);
		waitingBar.setVisibility(View.VISIBLE);
	}
	public void endWaiting(){
		progressBar.setVisibility(View.VISIBLE);
		waitingBar.setVisibility(View.GONE);
	}
}
