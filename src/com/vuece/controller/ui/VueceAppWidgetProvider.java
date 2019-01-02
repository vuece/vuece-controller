package com.vuece.controller.ui;


import java.io.File;

import com.vuece.controller.R;
import com.vuece.vtalk.android.util.Log;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;

public class VueceAppWidgetProvider extends AppWidgetProvider {
	private static final String TAG="vuece/VueceAppWidgetProvider";
    public static final String ACTION_REMOTE_BACKWARD = "com.vuece.controller.remote.Backward";
    public static final String ACTION_REMOTE_PLAYPAUSE = "com.vuece.controller.remote.PlayPause";
    public static final String ACTION_REMOTE_STOP = "com.vuece.controller.remote.Stop";
    public static final String ACTION_REMOTE_FORWARD = "com.vuece.controller.remote.Forward";
    public static final String ACTION_WIDGET_INIT = "com.vuece.controller.widget.INIT";
    public static final String ACTION_WIDGET_UPDATE = "com.vuece.controller.widget.UPDATE";
    public static final String ACTION_WIDGET_UPDATE_POSITION = "com.vuece.controller.widget.UPDATE_POSITION";
    public static final String VUECE_PACKAGE = "com.vuece.controller";
    public static final String VUECE_CONTROLLER = "com.vuece.controller.ui.MusicPlayerActivity";
    public static final String VUECE_WIDGET = "com.vuece.controller.ui.VueceAppWidgetProvider";
    public static final String VUECE_SERVICE = "com.vuece.controller.service.ControllerService";
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// TODO Auto-generated method stub
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "onUpdate "+appWidgetIds.length);
        /* init widget */
        Intent i = new Intent(ACTION_WIDGET_INIT);
        onReceive(context, i);

        i = new Intent();
        i.setClassName(VUECE_PACKAGE, VUECE_SERVICE);
        context.startService(i);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
       	Log.d(TAG, "VueceAppWidgetProvider received "+action);
        if (ACTION_WIDGET_INIT.equals(action)) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.miniplayerwidget);

            /* commands */
            Intent iBackward = new Intent(ACTION_REMOTE_BACKWARD);
            Intent iPlay = new Intent(ACTION_REMOTE_PLAYPAUSE);
            Intent iStop = new Intent(ACTION_REMOTE_STOP);
            Intent iForward = new Intent(ACTION_REMOTE_FORWARD);
            Intent iController = new Intent();
            iController.setClassName(VUECE_PACKAGE, VUECE_CONTROLLER);

            PendingIntent piBackward = PendingIntent.getBroadcast(context, 0, iBackward, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piPlay = PendingIntent.getBroadcast(context, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piStop = PendingIntent.getBroadcast(context, 0, iStop, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piForward = PendingIntent.getBroadcast(context, 0, iForward, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piVuece = PendingIntent.getActivity(context, 0, iController, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setOnClickPendingIntent(R.id.mp_backward, piBackward);
            views.setOnClickPendingIntent(R.id.mp_play_pause, piPlay);
            views.setOnClickPendingIntent(R.id.mp_stop, piStop);
            views.setOnClickPendingIntent(R.id.mp_forward, piForward);
            views.setOnClickPendingIntent(R.id.cover, piVuece);

            ComponentName widget = new ComponentName(context, VueceAppWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(widget, views);

        }else if (ACTION_WIDGET_UPDATE.equals(action)){
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.miniplayerwidget);
            Log.d(TAG, "remote views:"+views);

            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            boolean isplaying = intent.getBooleanExtra("isplaying", false);
            String preview = intent.getStringExtra("preview");

            views.setTextViewText(R.id.mp_songName, title);
            views.setTextViewText(R.id.mp_artist, artist);
            views.setImageViewResource(R.id.mp_play_pause, isplaying ? R.drawable.ic_action_playback_pause : R.drawable.ic_action_playback_play);
            if (preview != null){
            	File imageFile=new File(preview);
        		Log.d(TAG, "preview file exists?"+imageFile.exists());
        		if(imageFile.exists()){
        			Log.d(TAG, "preview file size:"+imageFile.length()+" "+imageFile.getAbsolutePath());
        		    Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        		    views.setImageViewBitmap(R.id.cover, myBitmap);
        		}                
        		
            }else
                views.setImageViewResource(R.id.cover, R.drawable.music_note);

            views.setViewVisibility(R.id.timeline_parent, artist != null && artist.length() > 0 ? View.VISIBLE : View.INVISIBLE);

//            iController.putExtra(START_FROM_NOTIFICATION, true);


            ComponentName widget = new ComponentName(context, VueceAppWidgetProvider.class);
            Log.d(TAG, "widget:"+widget);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(widget, views);
        	
        }else if (ACTION_WIDGET_UPDATE_POSITION.equals(action)){
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.miniplayerwidget);

            float pos = intent.getFloatExtra("position", 0f);
            views.setProgressBar(R.id.timeline, 100, (int) (100 * pos), false);

            ComponentName widget = new ComponentName(context, VueceAppWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(widget, views);
        	
        }else {
           	Log.d(TAG, "not customized message, going back to system");
        	super.onReceive(context, intent);
        }
	}

}
