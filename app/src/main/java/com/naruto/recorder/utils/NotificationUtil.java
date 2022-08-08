package com.naruto.recorder.utils;

import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/5 0005
 * @Note
 */
public class NotificationUtil {
    public static NotificationCompat.Action createAction(Context context, int icon, CharSequence title, String action) {
        PendingIntent intent = PendingIntent.getBroadcast(context, icon,
                IntentUtil.createBroadcastIntent(context, action, null),
                IntentUtil.defaultPendingIntentFlag());
        return new NotificationCompat.Action(icon, title, intent);
    }
}
