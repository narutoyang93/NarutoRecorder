package com.naruto.recorder.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.naruto.recorder.R;
import com.naruto.recorder.adapter.FileListAdapter;
import com.naruto.recorder.base.DataBindingActivity;
import com.naruto.recorder.databinding.ActivityPlayBinding;
import com.naruto.recorder.service.PlayService;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.MyTool;

import java.util.ArrayList;

/**
 * @Purpose 播放
 * @Author Naruto Yang
 * @CreateDate 2020/11/17 0017
 * @Note
 */
public class PlayActivity extends DataBindingActivity<ActivityPlayBinding> {
    private static final String INTENT_KEY_FILE_NAME = "fileName";
    private static final String INTENT_KEY_FILE_URI = "fileUri";
    private static final String INTENT_KEY_DURATION = "duration";

    private Uri fileUri;
    private String fileName;
    private PlayService.PlayBinder binder;
    private ServiceConnection connection;
    private boolean isPlaying = false;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_play;
    }

    @Override
    protected void init() {
        Intent intent = getIntent();
        fileUri = intent.getParcelableExtra(INTENT_KEY_FILE_URI);
        fileName = intent.getStringExtra(INTENT_KEY_FILE_NAME);
        setTitleBarTitle(fileName);
        dataBinding.setDuration(intent.getStringExtra(INTENT_KEY_DURATION));

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = (PlayService.PlayBinder) iBinder;
                binder.setIUpdateUI(new PlayService.IUpdateUI() {
                    @Override
                    public void updateState(boolean isPlaying) {
                        dataBinding.setIsPlaying(PlayActivity.this.isPlaying = isPlaying);
                    }

                    @Override
                    public void updateProgress(String time, int progress) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dataBinding.tvTime.setText(time);
                                dataBinding.seekBar.setProgress(progress);
                            }
                        });
                    }

                    @Override
                    public void showDialog(String message) {
                        DialogFactory.showHintDialog(message, PlayActivity.this);
                    }
                });
                dataBinding.ivPlay.callOnClick();//自动开始播放
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        dataBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) binder.seek(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        dataBinding.cbRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                binder.setRepeat(b);
            }
        });

        //启动服务
        bindService(PlayService.getLaunchIntent(this, fileUri, fileName), connection, BIND_AUTO_CREATE);
    }

    public void share(View view) {
        ArrayList<Uri> files = new ArrayList<>();
        files.add(fileUri);
        MyTool.shareFile(this, files);
    }


    public void playOrPause(View view) {
        if (isPlaying) {
            binder.pause();
        } else {
            binder.start();
        }
    }

    /**
     * 启动此页面
     *
     * @param activity
     * @param fileInfo
     */
    public static void launch(Activity activity, FileListAdapter.FileInfo fileInfo) {
        Intent intent = new Intent(activity, PlayActivity.class);
        intent.putExtra(INTENT_KEY_FILE_URI, fileInfo.uri);
        intent.putExtra(INTENT_KEY_FILE_NAME, fileInfo.name);
        intent.putExtra(INTENT_KEY_DURATION, fileInfo.duration);
        activity.startActivity(intent);
    }
}