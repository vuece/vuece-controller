package com.vuece.controller.ui;

import android.content.Context;
import android.os.AsyncTask;

import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.ToastMessageEvent;
import com.vuece.controller.model.SongItem;
import com.vuece.vtalk.android.jni.JabberClient;
import com.vuece.vtalk.android.util.Log;

public class PlaySongTask extends AsyncTask<Object, Void, Integer>{
    private static final String TAG = "PlaySongTask";
    protected Context service;
    protected JabberClient client;

	public PlaySongTask(Context service, JabberClient client) {
        this.service = service;
        this.client = client;
    }


	@Override
	protected Integer doInBackground(Object... params) {
		Log.d(TAG,"action "+params[0]);
		String action=(String)params[0];
		if (action.equalsIgnoreCase("play")) {
			Log.d(TAG," argument "+params[1]);
			SongItem songItem=(SongItem)params[1];
			return client.playSong(songItem);
		} else if (action.equalsIgnoreCase("next")) {
			return client.playNext();
		} else if (action.equalsIgnoreCase("previous")) {
			return client.playPrevious();
		} else if (action.equalsIgnoreCase("playpause")) {
			return client.playPause();
		} else if (action.equalsIgnoreCase("seek")) {
			Log.d(TAG," argument "+params[1]);
			Integer position=(Integer)params[1];
			return client.seekingStop(position);
		}
		return -2;
	}
    protected void onPostExecute(Integer result) {
        if (result==-1) {
        	BusProvider.getInstance().post(new ToastMessageEvent("Bufferring..."));
        } else if (result==-2) {
        	BusProvider.getInstance().post(new ToastMessageEvent("Wrong way..."));
        }
    }

}
