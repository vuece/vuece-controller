package com.vuece.controller.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.vuece.controller.R;
import com.vuece.vtalk.android.util.Log;

public class ToolbarFragment extends Fragment {
    ToolbarListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface ToolbarListener {
        /** Called by HeadlinesFragment when a list item is selected */
//        public void onSwitchButtonPressed();
        public void onRepeatButtonPressed();
        public void onShuffleButtonPressed();
        public void onPlayButtonPressed();
        public void onPreviousButtonPressed();
        public void onNextButtonPressed();
        public void onToolbarInitiate();
        public void onStopButtonPressed();
    }
	
	private static String TAG = "ToolbarFragment";
	private ImageButton playBtn;
	private ImageButton previousBtn;
	private ImageButton nextBtn;
	private ImageButton stopBtn;
	private TextView leftTitle;
	private TextView rightTitle;
	private ImageButton shuffleImage;
	private ImageButton repeatImage;

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
        return inflater.inflate(R.layout.toolbar, container, false);
    }
    @Override
	public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        this.playBtn=(ImageButton)getActivity().findViewById(R.id.pause);
        this.stopBtn=(ImageButton)getActivity().findViewById(R.id.stop);
        this.previousBtn=(ImageButton)getActivity().findViewById(R.id.prev);
        this.nextBtn=(ImageButton)getActivity().findViewById(R.id.next);
        this.leftTitle = (TextView)getActivity().findViewById(R.id.leftTitle);
        this.rightTitle = (TextView)getActivity().findViewById(R.id.rightTitle);
		shuffleImage = (ImageButton)getActivity().findViewById(R.id.shuffle);
		shuffleImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
            	Log.d(TAG, "shuffleImage onTouch");
            	mCallback.onShuffleButtonPressed();
            }
        });
		repeatImage = (ImageButton)getActivity().findViewById(R.id.repeat);
		repeatImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
            	Log.d(TAG, "repeatImage onTouch");
            	mCallback.onRepeatButtonPressed();
            }
        });
		playBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mCallback.onPlayButtonPressed();
            }
        });
		if(stopBtn!=null) stopBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mCallback.onStopButtonPressed();
            }
        });
		previousBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mCallback.onPreviousButtonPressed();
            }
        });
		nextBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mCallback.onNextButtonPressed();
            }
        });
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (ToolbarListener) activity;
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
        mCallback.onToolbarInitiate();
	}
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        Log.d(TAG, "onPause");
	}
    
	
	public void showShuffleOn(){
		shuffleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_playback_schuffle_on));
	}
	public void showShuffleOff(){
		shuffleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_playback_schuffle));
	}
	public void showRepeatOff(){
		repeatImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_playback_repeat));
	}
	public void showRepeatOnce(){
		repeatImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_playback_repeat_1));
	}
	public void showRepeatOn(){
		repeatImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_playback_repeat_on));
	}
	public void showPlayImage(){
		playBtn.setImageResource(R.drawable.ic_action_playback_play);
	}
	public void showPauseImage(){
		playBtn.setImageResource(R.drawable.ic_action_playback_pause);
	}
	public void showTitles(String left, String right){
        this.leftTitle.setText(left);
        this.rightTitle.setText(right);
	}
	public void clearTitles(){
        this.leftTitle.setText("");
        this.rightTitle.setText("");
	}
}
