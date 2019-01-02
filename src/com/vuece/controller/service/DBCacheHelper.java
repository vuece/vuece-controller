package com.vuece.controller.service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.vuece.controller.model.DirectoryItem;
import com.vuece.controller.model.DisplayItem;
import com.vuece.controller.model.SongItem;
import com.vuece.vtalk.android.jni.VTalkListener;
import com.vuece.vtalk.android.util.Log;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

public class DBCacheHelper  {
	
	private static String TAG = "vuece/DBCacheHelper";
	// Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    public static final String DATABASE_FILE = "/sdcard/vuece/db/VueceMediaCache.db";
    // Table name
    private static final String TABLE_NAME = "VueceQueryResult";
    
    private static final String ROOT_URI = "d41d8cd98f00b204e9800998ecf8427e";
    
    private SQLiteDatabase db;
 
    public DBCacheHelper() {
    }
	
	private void openDatabase(){
		db=SQLiteDatabase.openDatabase(DATABASE_FILE, null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
        String sql0="create table if not exists "+TABLE_NAME+"(id varchar2(32),chksum varchar2(32),uri varchar2(32),message text, UNIQUE(id, chksum, uri))";
        db.execSQL(sql0); 
	}
	public void closeDB() {
		if (db!=null&&db.isOpen()) db.close();
	}

	public void cleanup(String id, String chksum) {
        if (db==null||!db.isOpen()) openDatabase();
		db.delete(TABLE_NAME, " id like ? and not (chksum like ?)", new String[]{ id, chksum });
		
	}

	public void addCache(String id, String dbChecksum, String uri, String message) {
        if (db==null||!db.isOpen()) openDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("chksum", dbChecksum);
        values.put("uri", uri==null||uri==""?"/":uri);
        values.put("message", message);
        db.insert(TABLE_NAME, null, values);
	}
	public String getCache(String id, String dbChecksum, String uri) {
        if (db==null||!db.isOpen()) openDatabase();
        Log.d(TAG, "query cache -- id:"+id+"; checksum:"+dbChecksum+"; uri:"+uri);
        if (id==null||dbChecksum==null) 
        	return null;
       Cursor cursor = db.rawQuery("select message from "+TABLE_NAME+" where id like ? and chksum like ? and uri like ?",  new String[]{ id, dbChecksum, uri!=null&&uri.length()>0?uri:ROOT_URI });
  	 
       if (cursor.moveToFirst()) {
    	   return cursor.getString(0);
       }
       return null;
	}

}
