package com.vuece.controller.ui;

import com.vuece.controller.R;
import com.vuece.vtalk.android.util.Log;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class PreferencesActivity extends PreferenceActivity {
	private static String TAG="PreferencesActivity";
	public SharedPreferences mSharedPreferences;
		
	public OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {       
		
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		  Log.d(TAG, "A preference has been changed: "+key);
		  
//		            if (key.equals(R.string.pref_default_download_location_key)){
//		                                final Resources res = getResources();
//		                        String path = sharedPreferences.getString(res.getString(R.string.pref_default_download_location_key), "/mnt/sdcard/download");
//		                JabberClient.getInstance().setFileShareFolder(path);
//		                Log.d(TAG, "file share path is set to : "+path);
//		            }
		        }
		    };
		   
		    @Override
		    protected void onCreate(Bundle savedInstanceState) {
		        super.onCreate(savedInstanceState);
	        // Load the preferences from an XML resource
		        addPreferencesFromResource(R.xml.preferences);
		        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);;
		        mSharedPreferences.registerOnSharedPreferenceChangeListener(mListener);
		    }
		    @Override   
		    public void onDestroy() {
		        super.onDestroy();
		        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
		    }
}
