package com.naruto.recorder.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.naruto.recorder.activity.PlayActivity;
import com.naruto.recorder.base.ForegroundService;
import com.naruto.recorder.utils.MyTool;

import java.io.File;
import java.io.IOException;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/17 0017
 * @Note
 */
public class PlayService extends ForegroundService {
    private static final String INTENT_KEY_FILE_URI="fileUri";
    private static final String INTENT_KEY_FILE_NAME="fileName";

    private PlayBinder binder = new PlayBinder();
    private MediaPlayer mediaPlayer;
    private String fileName;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mediaPlayer = new MediaPlayer();
        Uri fileUri=intent.getParcelableExtra(INTENT_KEY_FILE_URI);
        fileName = intent.getStringExtra(INTENT_KEY_FILE_NAME);;
        try {
            mediaPlayer.setDataSource(this,fileUri);
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

    @Override
    protected int getNotificationId() {
        return ForegroundService.NOTIFICATION_ID_PLAY;
    }

    @Override
    protected PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, PlayActivity.class);
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
            updateNotification(notificationBuilder -> {
                notificationBuilder.setContentTitle(isPlaying ? "正在播放" : "已暂停")  //设置标题
                        .setContentText(fileName);//设置内容
            });
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


    /**
     *
     * @param context
     * @param fileUri
     * @param fileName
     * @return
     */
    public static Intent getLaunchIntent(Context context,Uri fileUri, String fileName){
        Intent intent = new Intent(context, PlayService.class);
        intent.putExtra(INTENT_KEY_FILE_URI, fileUri);
        intent.putExtra(INTENT_KEY_FILE_NAME, fileName);
        return intent;
    }
}
