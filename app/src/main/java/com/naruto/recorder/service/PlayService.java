package com.naruto.recorder.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.naruto.recorder.R;
import com.naruto.recorder.activity.PlayActivity;
import com.naruto.recorder.utils.MyTool;

import java.io.File;
import java.io.IOException;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/17 0017
 * @Note
 */
public class PlayService extends Service {
    public static final int NOTIFICATION_ID = 2;

    private PlayBinder binder = new PlayBinder();
    private MediaPlayer mediaPlayer;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private String fileName;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mediaPlayer = new MediaPlayer();
        String filePath = intent.getStringExtra("filePath");
        fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf("."));
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    binder.updateState(false);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            binder.iUpdateUI.showDialog("读取文件异常");
            mediaPlayer.release();
            mediaPlayer = null;
        }
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = MyTool.setForegroundService(this, PlayActivity.class, R.mipmap.ic_launcher, NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    /**
     * @Purpose Binder
     * @Author Naruto Yang
     * @CreateDate 2020/11/17 0017
     * @Note
     */
    public class PlayBinder extends Binder {
        private IUpdateUI iUpdateUI;

        public void setIUpdateUI(IUpdateUI IUpdateUI) {
            this.iUpdateUI = IUpdateUI;
        }

        /**
         * 开始播放
         */
        public void start() {
            mediaPlayer.start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            Thread.sleep(123);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mediaPlayer == null) return;
                        int time = mediaPlayer.getCurrentPosition();
                        iUpdateUI.updateProgress(MyTool.getTimeString(time), (int) ((float) time / mediaPlayer.getDuration() * 100));
                    } while (mediaPlayer.isPlaying());
                }
            }).start();
            updateState(true);
        }

        /**
         * 暂停
         */
        public void pause() {
            mediaPlayer.pause();
            updateState(false);
        }

        /**
         * 拖动
         */
        public void seek(int progress) {
            mediaPlayer.seekTo(mediaPlayer.getDuration() * progress / 100);
            if (!mediaPlayer.isPlaying()) start();//自动播放
        }

        /**
         * 设置循环播放
         *
         * @param repeat
         */
        public void setRepeat(boolean repeat) {
            mediaPlayer.setLooping(repeat);
        }

        /**
         * 更新状态
         */
        private void updateState(boolean isPlaying) {
            iUpdateUI.updateState(isPlaying);
            notificationBuilder.setContentTitle(isPlaying ? "正在播放" : "已暂停")  //设置标题
                    .setContentText(fileName);//设置内容
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }

    }


    /**
     * @Purpose 更新页面UI接口
     * @Author Naruto Yang
     * @CreateDate 2020/11/17 0017
     * @Note
     */
    public interface IUpdateUI {
        void updateState(boolean isPlaying);

        void updateProgress(String time, int progress);

        void showDialog(String message);
    }
}
