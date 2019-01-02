package com.vuece.controller.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.vuece.controller.R;
import com.vuece.controller.model.DirectoryItem;
import com.vuece.controller.model.DisplayItem;
import com.vuece.controller.model.SongItem;
import com.vuece.vtalk.android.jni.VTalkListener;
import com.vuece.vtalk.android.util.Log;

public class SongAdapter extends ArrayAdapter<DisplayItem> {
	   	private ArrayList<DisplayItem> items;
	    private static String TAG="SongAdapter";
	    private MusicPlayerActivity view;

	    public SongAdapter(MusicPlayerActivity view, int textViewResourceId, ArrayList<DisplayItem> items) {
	            super(view, textViewResourceId, items);
	            this.items = items;
	            this.view = view;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.media_item, null);
	            }
	            
	            DisplayItem o = items.get(position);
	            
	        	Log.d(TAG,"position:"+position +" uri:"+o.getUri());
	        	
	        	String currentPlayingUri=null;
	        	
	        	if (view.mService.getCurrentSongItem()!=null) {
	        		currentPlayingUri=view.mService.getCurrentSongItem().getUri();
	        		Log.d(TAG,"currentPlayingUri:"+currentPlayingUri);
	        	}
	        	
	            if (o != null) {
	            	ImageView itemIcon = (ImageView) v.findViewById(R.id.itemIcon);
	            	itemIcon.setImageResource(o.getIcon());
	            	
                    TextView nt = (TextView) v.findViewById(R.id.nametext);
                    TextView btl = (TextView) v.findViewById(R.id.bottomtextleft);
                    TextView btr = (TextView) v.findViewById(R.id.bottomtextright);
                    if (o instanceof DirectoryItem) {
                    	btl.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, (float) 1.0));
                    	btr.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, (float) 3.0));
                    }
                    ImageView currentState = (ImageView) v.findViewById(R.id.songCurrentState);
                    
                    if (nt != null) {
                    	
                    	Log.d(TAG, "Name: "+o.getTitle()+" isPlaying:"+(o.getUri().equalsIgnoreCase(currentPlayingUri)));
                    	
                		nt.setText(o.getTitle());
                		
                    	if (o.getUri().equalsIgnoreCase(currentPlayingUri)) {
//                    		nt.setText(new String(o.getTitle()+" (Current)"));
                    		if (view.mService.getCurrentPlayerState()==VTalkListener.MEDIA_PLAYER_STATE_PLAYING)
                    			currentState.setImageResource(R.drawable.ic_action_playback_play);
                    		else
                    			currentState.setImageResource(R.drawable.ic_action_playback_pause);
                    	}else{
                    		currentState.setImageDrawable(null);
                    	}
                    }
//                    Log.d(TAG,"btl:"+btl+";o.getSubTitleLeft():"+o.getSubTitleLeft());
                    if(btl != null){
                    	btl.setText(o.getSubTitleLeft());
                    }
                    
                    if(btr != null){
                    	btr.setText(o.getSubTitleRight());
                    }
	            }
	            
//	            v.setBackgroundColor((position & 1) == 1 ?Color.WHITE:Color.rgb(169, 216, 242));
//	            if((position & 1) == 1){
//	            	 v.setBackgroundResource(R.drawable.vuece_bkg_roster_item1);
//	            }else{
//	            	 v.setBackgroundResource(R.drawable.vuece_bkg_roster_item2);
//	            }
	           
	            v.setTag(o);
	            v.setOnClickListener(new OnClickListener(){

	    			public void onClick(View v) {
	    				
	    				DisplayItem item=(DisplayItem)v.getTag();
	    				
	    				Log.d(TAG,"view click:"+v.toString()+" "+item.getClass());
	    				
	    				if (item instanceof DirectoryItem) {
	    	    			view.mService.browseMusicDirectory((DirectoryItem)item);
	    				}else if (item instanceof SongItem) {
    						view.mService.playSong((SongItem)item);
//    						view.getAppContext().startAudioPlayerView();
    						
    						Log.d(TAG,"Calling view.showSong ");
    						
    						view.showSong();
    						
    						Log.d(TAG,"Calling view.showSong returned");
	    				}
	    			}
	            	
	            });
	            v.setOnTouchListener(new OnTouchListener(){

					@Override
					public boolean onTouch(View v, MotionEvent event) {
					    int action = event.getActionMasked();
					    
					    switch (action) {
					 
					        case MotionEvent.ACTION_DOWN:
					        	Log.d(TAG,"finger down");
					        	v.setBackgroundResource(R.drawable.vuecetheme_list_pressed_holo_light);
					            break;
					 
					        case MotionEvent.ACTION_UP:
					        	Log.d(TAG,"finger up");
					        	v.setBackgroundResource(0);
					        	break;
					        	
					        default:
					        	Log.d(TAG, "finger action:"+action);
					        	v.setBackgroundResource(0);
					    }
	            	
						return false;
					}
	            });
	            
	            return v;
	    }

}
