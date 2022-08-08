package com.naruto.recorder.service;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat.Action;

import com.naruto.recorder.Config;
import com.naruto.recorder.MyApplication;
import com.naruto.recorder.activity.MainActivity;
import com.naruto.recorder.base.ForegroundService;
import com.naruto.recorder.utils.Calculagraph;
import com.naruto.recorder.utils.FileUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class RecordService extends ForegroundService {
    public static final int STATE_READY = 0;//就绪
    public static final int STATE_RECORDING = 1;//录音中
    public static final int STATE_PAUSE = 2;//暂停

    private static final String DEFAULT_SUFFIX = ".m4a";
    private static final FileUtil.MediaType MEDIA_TYPE = FileUtil.MediaType.AUDIO;
    private static final String FOLDER_PATH = Config.DIR_RECORD;

    private RecordBinder binder = new RecordBinder();
    private MediaRecorder mediaRecorder;
    private Calculagraph calculagraph;//计时器
    private String fileName;//不带后缀
    private String fullFileName;//完整文件名，带后缀
    private int state = STATE_READY;
    private int lastSecondValue = -1;//上一次通知时的秒值，同一秒内只通知一次

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (mediaRecorder != null) {
            if (state == STATE_RECORDING) binder.save(fullFileName);//录音中出现异常，立即保存
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (calculagraph != null) calculagraph.destroy();
        super.onDestroy();
    }

    @Override
    protected int getNotificationId() {
        return ForegroundService.NOTIFICATION_ID_RECORD;
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return MainActivity.createPendingIntent(this, 10, null);
    }

    /**
     * @Purpose
     * @Author Naruto Yang
     * @CreateDate 2020/2/13 0013
     * @Note
     */
    public class RecordBinder extends Binder {
        private IUpdateUI IUpdateUI;

        public void setIUpdateUI(IUpdateUI IUpdateUI) {
            this.IUpdateUI = IUpdateUI;
        }

        /**
         * 开始
         */
        public void start() {
            if (mediaRecorder == null) mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);//设置输出文件的格式
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样
            mediaRecorder.setAudioChannels(2);
            mediaRecorder.setAudioEncodingBitRate(160000);
            mediaRecorder.setAudioSamplingRate(48000);

            fileName = (String) DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA));
            fullFileName = fileName + getSuffix();
            FileUtil.createAudioFileInExternalPublicSpace(FOLDER_PATH, fullFileName, uri -> {
                ContentResolver resolver = MyApplication.getContext().getContentResolver();
                try {
                    ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "w");
                    mediaRecorder.setOutputFile(pfd.getFileDescriptor());
                    mediaRecorder.prepare();
                    if (calculagraph == null) calculagraph = new Calculagraph() {
                        @Override
                        protected void updateUI(int hours, int minutes, int seconds, int milliseconds) {
                            IUpdateUI.updateCalculagraph(hours, minutes, seconds, milliseconds);
                            if (lastSecondValue == seconds) return;//忽略本次通知
                            //更新录音时长
                            updateNotification(notificationBuilder -> notificationBuilder.setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds)));
                        }
                    };
                    mediaRecorder.start();//开始
                    calculagraph.start();//计时
                } catch (Exception e) {
                    e.printStackTrace();
                    MyApplication.toast("录音文件异常");
                    return;
                }
                updateState(state = STATE_RECORDING);
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        /**
         * 暂停
         */
        public void pause() {
            mediaRecorder.pause();
            calculagraph.pause();
            updateState(state = STATE_PAUSE);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        /**
         * 继续
         */
        public void resume() {
            mediaRecorder.resume();
            calculagraph.resume();
            updateState(state = STATE_RECORDING);
        }

        /**
         * 停止
         */
        public void stop() {
            //正在录音需要先暂停
            if (state == STATE_RECORDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pause();
            }

            IUpdateUI.askForSave(fileName);
        }

        private void reset() {
            IUpdateUI.updateState(state = STATE_READY);
            calculagraph.reset();
            lastSecondValue = -1;
            stopSelf();
        }

        /**
         * 保存
         *
         * @param newName 新文件名
         */
        public void save(String newName) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                if (!fileName.equals(newName)) {//重命名
                    if (!FileUtil.renameFileInExternalPublicSpace(MEDIA_TYPE, FOLDER_PATH, fullFileName, newName + getSuffix()))
                        throw new Exception("重命名失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
                IUpdateUI.showDialog("保存异常");
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
                return;
            }

            Toast.makeText(RecordService.this, "保存成功", Toast.LENGTH_SHORT).show();
            reset();
        }

        /**
         * 不保存，选择删除
         */
        public void delete() {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                FileUtil.deleteFileInExternalPublicSpace(MEDIA_TYPE, FOLDER_PATH, fullFileName);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            reset();
        }

        private void updateState(int state0) {
            IUpdateUI.updateState(state = state0);
            String stateString;
            ArrayList<Action> mActions = new ArrayList<>();
            switch (state0) {
                case STATE_PAUSE:
                    stateString = "已暂停";
                    mActions.add(MainActivity.createResumeActionIntent(RecordService.this));
                    mActions.add(MainActivity.createStopActionIntent(RecordService.this));
                    break;
                case STATE_RECORDING:
                    stateString = "正在录音";
                    mActions.add(MainActivity.createPauseActionIntent(RecordService.this));
                    mActions.add(MainActivity.createStopActionIntent(RecordService.this));
                    break;
                default:
                    stateString = "已就绪";
                    break;
            }
            //更新录音状态
            updateNotification(notificationBuilder -> {
                notificationBuilder.setContentTitle(stateString);
                //通知上的按钮
                notificationBuilder.clearActions();
                for (Action action : mActions) {
                    notificationBuilder.addAction(action);
                }
            });
        }
    }

    public static String getSuffix() {
        return DEFAULT_SUFFIX;
    }

    /**
     * @Purpose 更新页面UI接口
     * @Author Naruto Yang
     * @CreateDate 2020/2/13 0013
     * @Note
     */
    public interface IUpdateUI {
        void updateState(int state);

        void showDialog(String message);

        /**
         * 更新计时器
         *
         * @param hours
         * @param minutes
         * @param seconds
         */
        void updateCalculagraph(int hours, int minutes, int seconds, int milliseconds);

        /**
         * 询问是否保存
         */
        void askForSave(String fileName);
    }
}
