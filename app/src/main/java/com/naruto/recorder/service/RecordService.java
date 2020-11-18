package com.naruto.recorder.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.naruto.recorder.activity.MainActivity;
import com.naruto.recorder.R;
import com.naruto.recorder.utils.Calculagraph;
import com.naruto.recorder.utils.MyTool;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_DCIM;

public class RecordService extends Service {
    public static final int STATE_READY = 0;//就绪
    public static final int STATE_RECORDING = 1;//录音中
    public static final int STATE_PAUSE = 2;//暂停

    public static final int NOTIFICATION_ID = 1;
    private static final String DEFAULT_SAVE_FOLDER = Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath() + "/sound record/";
    private static final String DEFAULT_SUFFIX = ".m4a";
    private RecordBinder binder = new RecordBinder();
    private MediaRecorder mediaRecorder;
    private Calculagraph calculagraph;//计时器
    private static final String TAG = "RecordService";
    private String fileName;
    private int state = STATE_READY;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private int lastSecondValue = -1;//上一次通知时的秒值，同一秒内只通知一次

    public RecordService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = MyTool.setForegroundService(this, MainActivity.class, R.mipmap.ic_launcher, NOTIFICATION_ID);
    }

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
            if (state == STATE_RECORDING) binder.save(fileName);//录音中出现异常，立即保存
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (calculagraph != null) calculagraph.destroy();
        super.onDestroy();
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
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioChannels(2);
            mediaRecorder.setAudioEncodingBitRate(160000);
            mediaRecorder.setAudioSamplingRate(48000);

            File folder = new File(getSaveFolderPath());
            if (!folder.exists()) folder.mkdirs();
            /* ③准备 */
            fileName = (String) DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA));
            mediaRecorder.setOutputFile(getNewFilePath(fileName));
            boolean b = true;
            try {
                mediaRecorder.prepare();
                if (calculagraph == null) calculagraph = new Calculagraph() {
                    @Override
                    protected void updateUI(int hours, int minutes, int seconds, int milliseconds) {
                        IUpdateUI.updateCalculagraph(hours, minutes, seconds, milliseconds);
                        if (lastSecondValue == seconds) return;//忽略本次通知
                        notificationBuilder.setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds));//更新录音时长
                        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    }
                };
                /* ④开始 */
                mediaRecorder.start();
                calculagraph.start();//计时
            } catch (IOException e) {
                b = false;
                e.printStackTrace();
            }
            if (b)
                updateState(state = STATE_RECORDING);
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
            boolean b = true;

            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                if (!fileName.equals(newName)) {//重命名
                    File old = new File(getNewFilePath(fileName));
                    old.renameTo(new File(getNewFilePath(newName)));
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                b = false;
                IUpdateUI.showDialog("保存异常");
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (b) {
                Toast.makeText(RecordService.this, "保存成功", Toast.LENGTH_SHORT).show();
            }
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
                new File(getNewFilePath(fileName)).delete();
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
            switch (state0) {
                case STATE_PAUSE:
                    stateString = "已暂停";
                    break;
                case STATE_RECORDING:
                    stateString = "正在录音";
                    break;
                default:
                    stateString = "已就绪";
                    break;
            }
            notificationBuilder.setContentTitle(stateString); //更新录音状态
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            //防止更新冲突导致更新失败
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            }, 123);
        }
    }

    public static String getSaveFolderPath() {
        return DEFAULT_SAVE_FOLDER;
    }

    public static String getSuffix() {
        return DEFAULT_SUFFIX;
    }

    /**
     * @param fileName 不含后缀
     * @return
     */
    public static String getNewFilePath(String fileName) {
        return getSaveFolderPath() + fileName + getSuffix();
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
