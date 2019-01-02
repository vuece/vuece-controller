package com.vuece.vtalk.android.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vuece.vtalk.android.model.RosterEntry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Configuration;

public class JabberUtils {
	public static String normalizeJid(String jid){
		if (jid!=null&&jid.indexOf("/")>-1){
			return jid.substring(0,jid.indexOf("/"));
		}
		return jid;
	}
	public static String getDisplayName(String jid, List<RosterEntry> roster){
		String bareJid=JabberUtils.normalizeJid(jid);
		if (roster==null) return bareJid;
		for (RosterEntry re:roster){
			if (re.getBareJid().equals(bareJid)){
				return re.getDisplayName();
			}
		}
		return bareJid;
	}
	public static String genFileShareId() {
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyyMMddhhmmssSSS");
    	return formatter.format(new Date());
	}
	
	public static String getTimeFormat(int length){
    	int hours = length / 3600;
    	int minutes = (length % 3600) / 60;
    	int seconds = length % 60;
    	return (hours>9?hours:("0"+hours)) + ":" + (minutes>9?minutes:("0"+minutes)) + ":" + (seconds>9?seconds:("0"+seconds));
	}
	public static String getTimeFormatShort(int length){
		if (length<3600) {
	    	int minutes = (length % 3600) / 60;
	    	int seconds = length % 60;
	    	return (minutes>9?minutes:("0"+minutes)) + ":" + (seconds>9?seconds:("0"+seconds));
		}else{
	    	return getTimeFormat(length);
		}
	}
	public static boolean isXLargeScreen(Context context) {
	    return (context.getResources().getConfiguration().screenLayout
	    & Configuration.SCREENLAYOUT_SIZE_MASK)
	    >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	} 	
    public static boolean isFroyoOrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
    }

    public static boolean isGingerbreadOrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean isHoneycombOrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isICSOrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean isJellyBeanOrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
    }
    public static boolean isJellyBeanMR2OrLater()
    {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
//	public static String getPassword(Context context){
//    	AccountManager accountManager = AccountManager.get(context);
//    	Account[] accounts = accountManager.getAccountsByType("com.vuece.vtalk");
//        Account account;
//        if (accounts.length > 0) {
//          account = accounts[0];      
//        } else {
//          account = null;
//        }
//        accountManager.
//        return accountManager.getPassword(account);
//
//	}
}
