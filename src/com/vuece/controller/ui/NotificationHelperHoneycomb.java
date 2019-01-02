package com.vuece.controller.ui;

import com.vuece.controller.R;
import com.vuece.controller.core.ControllerApplication;
import com.vuece.controller.service.ControllerService;
import com.vuece.controller.service.NotificationReceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationHelperHoneycomb extends NotificationHelper {

	@Override
	public void showNotification(ControllerService service) {
		Intent intent = new Intent(service.getApplicationContext(), MusicPlayerActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(service, 0, intent, 0);
		
		Notification noti = new Notification.Builder(service.getApplicationContext())
	        .setContentTitle(service.getCurrentSongItem().getName())
	        .setContentText(service.getCurrentSongItem().getArtist()).setSmallIcon(R.drawable.ic_launcher)
	        .setContentIntent(pIntent).getNotification();
//	        .addAction(android.R.drawable.ic_media_previous, "Call", pIntent)
//	        .addAction(android.R.drawable.ic_media_pause, "More", pIntent)
//	        .addAction(android.R.drawable.ic_media_next, "Three", pIntent).build();
    
  
		NotificationManager notificationManager = 
		  (NotificationManager) service.getApplicationContext().getSystemService(ControllerApplication.NOTIFICATION_SERVICE);

		// Hide the notification after its selected
		noti.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

		notificationManager.notify(NOTIFICATION_ID, noti); 

	}


}
