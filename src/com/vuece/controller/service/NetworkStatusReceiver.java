package com.vuece.controller.service;

import com.vuece.vtalk.android.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStatusReceiver extends BroadcastReceiver {

	private static String TAG="NetworkStatusReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager cm =
	        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	 

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                         activeNetwork.isConnectedOrConnecting();
		       
		Intent i = null;
		if (isConnected) {
		 	Log.d(TAG, "network is connected");
		 	i = new Intent(ControllerService.ACTION_NETWORK_CONNECTED);
		}else{
        	Log.d(TAG, "network is disconnected");
        	i = new Intent(ControllerService.ACTION_NETWORK_DISCONNECTED);
		}
        if (isOrderedBroadcast())
            abortBroadcast();
        if(i != null)
            context.sendBroadcast(i);

		
	}

}
