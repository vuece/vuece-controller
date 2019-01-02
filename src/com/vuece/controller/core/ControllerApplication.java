package com.vuece.controller.core;

import java.io.File;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Intent;

import com.vuece.controller.R;
import com.vuece.controller.service.ControllerService;
import com.vuece.vtalk.android.jni.VTalkListener;
import com.vuece.vtalk.android.util.Log;

@ReportsCrashes(
        formKey = "",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
	    formUri = "https://bugreport.vuece.com/acra-controller/_design/acra-storage/_update/report",
	    formUriBasicAuthLogin = "ieforyoundesprightsameds",
	    formUriBasicAuthPassword = "CcvyK3Iu5EKli7cs8ftCWAya",
	    disableSSLCertValidation = true,
        
        // Your usual ACRA configuration
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
        )

public class ControllerApplication extends Application {

	private static String TAG = "vuece/ControllerApplication";
    private ControllerService service;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG, "onCreate");
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        // start service as background
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, ControllerService.class);
        startService(intent);
        //check tmp dir
        File tmpDir=new File(VTalkListener.AUDIO_CACHE_DIR);
        if (!tmpDir.exists()) {
        	tmpDir.mkdirs();
        }
        tmpDir=new File("/sdcard/vuece/tmp/video");
        if (!tmpDir.exists()) {
        	tmpDir.mkdirs();
        }
        tmpDir=new File(VTalkListener.DB_CACHE_DIR);
        if (!tmpDir.exists()) {
        	tmpDir.mkdirs();
        }
	}

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		Log.d(TAG, "onTerminate");
	}

	public void setService(ControllerService service) {
		this.service = service;
	}

	public ControllerService getService() {
		return service;
	}
	





}
