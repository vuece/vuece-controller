package com.vuece.controller.ui;

import com.vuece.controller.core.ControllerApplication;
import com.vuece.controller.service.ControllerService;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public abstract class NotificationHelper {

	protected static int NOTIFICATION_ID=0;
    public static NotificationHelper createInstance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return new NotificationHelperHoneycomb();
        } else {
            return new NotificationHelperEclair();
        }
    }

	public abstract void showNotification(ControllerService service);
	public void hideNotification(ControllerService service){
		NotificationManager notificationManager = 
				  (NotificationManager) service.getApplicationContext().getSystemService(ControllerApplication.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);

	}
	
}
