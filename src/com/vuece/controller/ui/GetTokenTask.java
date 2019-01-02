package com.vuece.controller.ui;

import java.io.IOException;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.ClientStateChangedEvent;
import com.vuece.vtalk.android.jni.VTalkListener;
import com.vuece.vtalk.android.util.Log;

import android.content.Context;
import android.os.AsyncTask;

public class GetTokenTask extends AsyncTask<Void, Void, Void>{
    private static final String TAG = "GetTokenTask";
    public static final String SCOPE = "oauth2:https://www.googleapis.com/auth/googletalk";
    protected Context mActivity;
    protected LoginFragment mFragment;

    protected String mScope;
    protected String mEmail;

	
	GetTokenTask(Context activity, LoginFragment fragment, String email) {
        this.mActivity = activity;
        this.mFragment = fragment;
        this.mEmail = email;
    }


	@Override
	protected Void doInBackground(Void... params) {
		try {
			Log.d(TAG,"getting OAUTH token for "+mEmail);
			String token = GoogleAuthUtil.getToken(mActivity, mEmail, SCOPE);
			Log.d(TAG,"OAUTH token:"+token);
			mFragment.loginWithToken(token);
		} catch (UserRecoverableAuthException userRecoverableException) {
		// GooglePlayServices.apk is either old, disabled, or not present, which is
		// recoverable, so we need to show the user some UI through the activity.
			mFragment.handleException(userRecoverableException);
		} catch (GoogleAuthException fatalException) {
			mFragment.oauthFailed("Unrecoverable error " + fatalException.getMessage());
			Log.d(TAG, "Unrecoverable error " + fatalException.getMessage(), fatalException);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			mFragment.oauthFailed("IO error " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

}
