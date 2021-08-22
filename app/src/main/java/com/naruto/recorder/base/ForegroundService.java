package com.naruto.recorder.base;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.core.app.NotificationCompat;

import com.naruto.recorder.InterfaceFactory;
import com.naruto.recorder.R;
import com.naruto.recorder.utils.ServiceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Description 前台服务
 * @Author Naruto Yang
 * @CreateDate 2021/6/24 0024
 * @Note
 */
public abstract class ForegroundService extends Service {
    @IntDef({NOTIFICATION_ID_RECORD, NOTIFICATION_ID_PLAY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NotificationId {
    }

    public static final int NOTIFICATION_ID_RECORD = 1;
    public static final int NOTIFICATION_ID_PLAY = 2;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = initNotification();
    }

    /**
     * 初始化通知
     *
     * @return
     */
    protected NotificationCompat.Builder initNotification() {
        return ServiceUtils.setForegroundService(this, getPendingIntent(), R.drawable.ic_logo_notification, getNotificationId());
    }

    /**
     * 更新通知
     *
     * @param operation
     */
    protected void updateNotification(InterfaceFactory.Operation<NotificationCompat.Builder> operation) {
        operation.done(notificationBuilder);
        notificationManager.notify(getNotificationId(), notificationBuilder.build());
    }


    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    protected abstract @NotificationId
    int getNotificationId();

    protected abstract PendingIntent getPendingIntent();

    /**
     * 启动前台服务
     *
     * @param context
     * @param serviceIntent
     */
    public static void launch(Context context, Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
