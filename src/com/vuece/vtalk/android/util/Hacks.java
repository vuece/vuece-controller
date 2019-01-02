package com.vuece.vtalk.android.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public class Hacks {

//	public static boolean isGalaxyS() {
//		return Build.DEVICE.startsWith("GT-I900") || Build.DEVICE.startsWith("GT-P1000");
//	}

/*	private static final boolean log(final String msg) {
		Log.d("Linphone", msg);
		return true;
	}*/

	/* Not working as now
	 * Calling from Galaxy S to PC is "usable" even with no hack; other side is not even with this one*/
	public static void galaxySSwitchToCallStreamUnMuteLowerVolume(Context context) {
		if (!isGalaxyS()) return;
		Log.d("Hacks","unmute volume for galaxy s");
    	AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		// Switch to call audio channel (Galaxy S)
		am.setSpeakerphoneOn(false);
		sleep(200);

		// Lower volume
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);

		// Another way to select call channel
		am.setMode(AudioManager.MODE_NORMAL);
		sleep(200);

		// Mic is muted if not doing this
		am.setMicrophoneMute(true);
		sleep(200);
		am.setMicrophoneMute(false);
		sleep(200);
	}

	private static final void sleep(int time) {
		try  {
			Thread.sleep(time);
		} catch(InterruptedException ie){}
	}

	public static void dumpDeviceInformation() {
		StringBuilder sb = new StringBuilder(" ==== Phone information dump ====\n");
		sb.append("DEVICE=").append(Build.DEVICE).append("\n");
		sb.append("MODEL=").append(Build.MODEL).append("\n");
		//MANUFACTURER doesn't exist in android 1.5.
		//sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append("\n");
		sb.append("SDK=").append(Build.VERSION.SDK);
		
		Log.d("Hacks", sb.toString());
	}
	
    public static boolean isGalaxySOrTabWithFrontCamera() {
        return isGalaxySOrTab() && !isGalaxySOrTabWithoutFrontCamera();
}
private static boolean isGalaxySOrTabWithoutFrontCamera() {
        return isSC02B() || isSGHI896();
}


public static boolean isGalaxySOrTab() {
        return isGalaxyS() || isGalaxyTab();
}

public static boolean isGalaxyTab() {
        return isGTP1000();
}
private static boolean isGalaxyS() {
        return isGT9000() || isSC02B() || isSGHI896() || isSPHD700();
}

public static final boolean hasTwoCamerasRear0Front1() {
        return isSPHD700() || isADR6400();
}

// HTC
private static final boolean isADR6400() {
        return Build.MODEL.startsWith("ADR6400") || Build.DEVICE.startsWith("ADR6400");
} // HTC Thunderbolt

// Galaxy S variants
private static final boolean isSPHD700() {return Build.DEVICE.startsWith("SPH-D700");} // Epic
private static boolean isSGHI896() {return Build.DEVICE.startsWith("SGH-I896");} // Captivate
private static boolean isGT9000() {return Build.DEVICE.startsWith("GT-I900");} // Galaxy S
private static boolean isSC02B() {return Build.DEVICE.startsWith("SC-02B");} // Docomo
private static boolean isGTP1000() {return Build.DEVICE.startsWith("GT-P1000");} // Tab

}
