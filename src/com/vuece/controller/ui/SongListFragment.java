package com.vuece.controller.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.squareup.otto.Subscribe;
import com.vuece.controller.R;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.PlayerStateChangedEvent;
import com.vuece.controller.event.SongListChangedEvent;
import com.vuece.vtalk.android.util.Log;

public class SongListFragment extends Fragment {
    OnSongSelectedListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnSongSelectedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onSongSelected(int position);
    }
	
	private static String TAG = "SongListFragment";
    private SongAdapter songAdapter;
    private View mSelectedItemView = null; 
    
    public void setSongAdapter(SongAdapter songAdapter) {
		this.songAdapter = songAdapter;
	}

//	@Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // We need to use a different list item layout for devices older than Honeycomb
////        int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
////                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
////
////        // Create an array adapter for the list view, using the Ipsum headlines array
////        setListAdapter(new ArrayAdapter<String>(getActivity(), layout, Ipsum.Headlines));
//        Log.d(TAG, "activity class:"+getActivity().getClass());
//        setListAdapter(songAdapter);
//    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
        if (savedInstanceState != null) {
//          mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
      }
      

      // Inflate the layout for this fragment
      View thisView=inflater.inflate(R.layout.song_list, container, false);
      ListView listview=(ListView)thisView.findViewById(R.id.songList);
      View empty=thisView.findViewById(R.id.empty_song_list_stub);
      listview.setEmptyView(empty);
      listview.setAdapter(songAdapter);
      listview.setFastScrollEnabled(true);
      return thisView;//inflater.inflate(R.layout.waiting, container, false);
	}

//	@Override
//	public void onViewCreated(View view, Bundle savedInstanceState) {
//		// TODO Auto-generated method stub
//		super.onViewCreated(view, savedInstanceState);
//		((ListView)view).setSelector(R.drawable.vuecetheme_list_selector_holo_light);
//	}

//	@Override
//	public void onActivityCreated(Bundle savedInstanceState) {
//		// TODO Auto-generated method stub
//		super.onActivityCreated(savedInstanceState);
//		getListView().setSelector(R.drawable.vuecetheme_list_selector_holo_light);
//	}

//	@Override
//	public void onStart() {
//        super.onStart();
//        Log.d(TAG, "onStart");
//        // Bind to LocalService
//        Intent intent = new Intent(getActivity(), ControllerService.class);
//
//        // When in two-pane layout, set the listview to highlight the selected list item
//        // (We do this during onStart because at the point the listview is available.)
////        if (getFragmentManager().findFragmentById(R.id.fragment_container_toolbar) != null) {
////            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
////        }
//        getListView().setFastScrollEnabled(true);
//    }

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnSongSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSongSelectedListener");
        }
    }
    
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        Log.d(TAG, "onResume");
        BusProvider.getInstance().register(this);
        songAdapter.notifyDataSetChanged();
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

//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        // Notify the parent activity of selected item
//        mCallback.onSongSelected(position);
//        
//        // Set the item as checked to be highlighted when in two-pane layout
//        //getListView().setItemChecked(position, true);
//    }
	@Subscribe public void onPlayerStateChanged(PlayerStateChangedEvent event){
//		int currentPlayerState=event.getState();
//		if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_PLAYING){
////		}else if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_PAUSED){
//		}else if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_BUFFERING){
//		}else if (currentPlayerState==VTalkListener.MEDIA_PLAYER_STATE_STOPPED){
//		}
    	songAdapter.notifyDataSetChanged();
	}
	@Subscribe public void onSongListChanged(SongListChangedEvent event){
		songAdapter.notifyDataSetChanged();
	}
}
