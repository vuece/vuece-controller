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
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

public class NotificationHelperEclair extends NotificationHelper {

	@Override
	public void showNotification(ControllerService service) {
		Intent intent = new Intent(service.getApplicationContext(), NotificationReceiver.class);
		PendingIntent pIntent = PendingIntent.getActivity(service.getApplicationContext(), 0, intent, 0);
		String text=service.getCurrentSongItem().getName();
		CharSequence tickerText = buildTickerMessage(service.getApplicationContext(), service.getCurrentSongItem().getName(), text);
		Notification noti = new Notification(R.drawable.ic_launcher, tickerText, System.currentTimeMillis());
//	        .addAction(android.R.drawable.ic_media_previous, "Call", pIntent)
//	        .addAction(android.R.drawable.ic_media_pause, "More", pIntent)
//	        .addAction(android.R.drawable.ic_media_next, "Three", pIntent).build();
  
		NotificationManager notificationManager = 
		  (NotificationManager) service.getApplicationContext().getSystemService(ControllerApplication.NOTIFICATION_SERVICE);

		// Hide the notification after its selected
		noti.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

//		notificationManager.notify(NOTIFICATION_ID, noti); 

	}
	protected static CharSequence buildTickerMessage(
				            Context context, String address, String body) {
				        String displayAddress = address;
				
				        StringBuilder buf = new StringBuilder(
				                displayAddress == null
				                ? ""
				                : displayAddress.replace('\n', ' ').replace('\r', ' '));
				        buf.append(':').append(' ');
				
				        int offset = buf.length();
				
				        if (!TextUtils.isEmpty(body)) {
				            body = body.replace('\n', ' ').replace('\r', ' ');
				            buf.append(body);
				        }
				
				        SpannableString spanText = new SpannableString(buf.toString());
				        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, offset,
				                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				
				        return spanText;
				    }

}
