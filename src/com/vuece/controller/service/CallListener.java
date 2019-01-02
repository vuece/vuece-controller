package com.vuece.controller.service;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.vuece.vtalk.android.util.Log;

public class CallListener extends PhoneStateListener {
	private static String TAG = "CallListener";
	private Context context;
	public CallListener(Context context) {
		this.context=context;
	}
	public void onCallStateChanged(int state,String incomingNumber){
		switch(state){
		    case TelephonyManager.CALL_STATE_IDLE:
		      Log.d(TAG, "IDLE");
		      break;
		    case TelephonyManager.CALL_STATE_OFFHOOK:
		      Log.d(TAG, "OFFHOOK");
		  	  break;
		    case TelephonyManager.CALL_STATE_RINGING:
		      Log.d(TAG, "RINGING");
//				Intent serviceIntent = new Intent(context, ControllerService.class);
//				serviceIntent.setFlags(ControllerService.FLAG_RECEIVER_RINGING);
//				context.startService(serviceIntent);
				Intent i = new Intent(ControllerService.ACTION_REMOTE_PAUSE);
				context.sendBroadcast(i);
			break;
	    }
	} 
}
