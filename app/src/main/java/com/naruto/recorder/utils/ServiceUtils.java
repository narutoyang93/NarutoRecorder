package com.naruto.recorder.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.naruto.recorder.R;


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/6/24 0024
 * @Note
 */
public class ServiceUtils {

    /**
     * 创建通知渠道
     *
     * @param context
     * @return
     */
    private static String createNotificationChannel(Context context) {
        final String CHANNEL_ID = context.getPackageName() + ".notification.channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), importance);
            channel.setDescription("渠道描述");
            channel.setSound(null, null);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
        }

        return CHANNEL_ID;
    }

    /**
     * 创建通知
     *
     * @param service
     * @param pendingIntent
     * @return
     */
    public static NotificationCompat.Builder createNotificationBuilder(Service service, PendingIntent pendingIntent) {
        //创建通知
        return new NotificationCompat.Builder(service, createNotificationChannel(service))
                .setContentIntent(pendingIntent)
                /* .setContentTitle("这是测试通知标题")  //设置标题
              .setContentText("这是测试通知内容") //设置内容*/
                .setWhen(System.currentTimeMillis());
    }

    /**
     * 设置Service为前台服务
     *
     * @param service
     * @param pendingIntent  点击通知将执行的意图
     * @param iconRes        通知图标
     * @param notificationId
     * @return
     */
    public static NotificationCompat.Builder setForegroundService(Service service, PendingIntent pendingIntent
            , @DrawableRes int iconRes, int notificationId, NotificationCompat.Action... actions) {
        NotificationCompat.Builder builder = createNotificationBuilder(service, pendingIntent).setSmallIcon(iconRes);
        if (actions != null && actions.length > 0) {
            for (NotificationCompat.Action action : actions) {
                builder.addAction(action);
            }
        }
        setForegroundService(service, notificationId, builder);
        return builder;
    }

    /**
     * 设置Service为前台服务
     *
     * @param service
     * @param pendingIntent  点击通知将执行的意图
     * @param notificationId
     * @param remoteViews
     * @return
     */
    public static NotificationCompat.Builder setForegroundService(Service service, PendingIntent pendingIntent
            , int notificationId, RemoteViews remoteViews) {
        NotificationCompat.Builder builder = createNotificationBuilder(service, pendingIntent).setContent(remoteViews);
        setForegroundService(service, notificationId, builder);
        return builder;
    }

    /**
     * 设置Service为前台服务
     *
     * @param service
     * @param notificationId
     * @param builder
     */
    public static void setForegroundService(Service service, int notificationId, NotificationCompat.Builder builder) {
        service.startForeground(notificationId, builder.build());
    }
}
