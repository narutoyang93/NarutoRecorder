package com.naruto.recorder.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naruto.recorder.R;
import com.naruto.recorder.base.DataBindingActivity;
import com.naruto.recorder.databinding.ActivityMainBinding;
import com.naruto.recorder.databinding.DialogSaveBinding;
import com.naruto.recorder.service.RecordService;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.MyTool;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/12 0012
 * @Note
 */
public class MainActivity extends DataBindingActivity<ActivityMainBinding> {
    public static final int REQUEST_CODE_PERMISSION_RECODE = 100;
    public static final int REQUEST_CODE_PERMISSION_SHOW_FILE = 101;
    private int state = 0;//状态
    private RecordService.RecordBinder binder;
    private ServiceConnection connection;
    private Dialog saveDialog;//保存弹窗
    private DialogSaveBinding saveBinding;
    private boolean isNeedResumeWhenBack = false;//保存弹窗返回是否需要继续录音（当按下“完成”按钮时正在录音，则返回后需要继续录音）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            state = savedInstanceState.getInt("state", 0);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (RecordService.RecordBinder) service;
                binder.setIUpdateUI(new RecordService.IUpdateUI() {

                    @Override
                    public void updateState(int state) {
                        changeState(state);
                    }

                    @Override
                    public void showDialog(String message) {
                        DialogFactory.showHintDialog(message, MainActivity.this);
                    }

                    @Override
                    public void updateCalculagraph(int hours, int minutes, int seconds, int milliseconds) {
                        String text = (hours > 0 ? String.format("%02d:", hours) : "") + String.format("%02d:%02d:%02d", minutes, seconds, milliseconds / 10);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dataBinding.tvTime.setText(text);
                            }
                        });
                    }

                    @Override
                    public void askForSave(String fileName) {
                        showSaveDialog(fileName);
                    }
                });
                binder.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getCharSequenceExtra("tag") != null)
            pauseOrResume(null);
    }

    /**
     * 开始
     */
    public void start() {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_RECODE, new RequestPermissionsCallBack() {
            @Override
            public void onGranted() {
                Intent intent = new Intent(MainActivity.this, RecordService.class);
/*        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }*/
                bindService(intent, connection, BIND_AUTO_CREATE);
            }
        });
    }

    /**
     * 暂停
     *
     * @param view
     */

    public void pauseOrResume(View view) {
        switch (state) {
            case RecordService.STATE_READY://已就绪
                start();//开始录音
                break;
            case RecordService.STATE_RECORDING://录音中
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //高版本才有暂停功能
                    binder.pause();//暂停
                } else {//提示
                    toast("当前android版本过低，暂不支持暂停功能");
                }
                break;
            default://已暂停
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binder.resume();//继续
                }
                break;
        }
    }

    /**
     * 完成
     *
     * @param view
     */
    public void complete(View view) {
        isNeedResumeWhenBack = state == RecordService.STATE_RECORDING;
        binder.stop();
    }

    /**
     * 切换状态
     *
     * @param newState
     */
    private void changeState(int newState) {
        if (newState != state) {
            dataBinding.setState(state = newState);
        }
    }

    /**
     * 显示保存弹窗
     *
     * @param fileName
     */
    private void showSaveDialog(String fileName) {
        if (saveDialog == null) createSaveDialog();
        saveBinding.setValue(fileName);
        saveBinding.include.editText.post(new Runnable() {
            @Override
            public void run() {
                saveBinding.include.editText.selectAll();
            }
        });
        saveDialog.show();
    }


    /**
     * 创建保存弹窗
     */
    private void createSaveDialog() {
        //创建弹窗
        saveBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_save, (ViewGroup) rootView, false);
        saveDialog = MyTool.createBottomInputDialog(this, saveBinding.getRoot());
        saveDialog.setCancelable(true);

        //设置点击事件
        saveBinding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String v = saveBinding.getValue().trim();
                if (MyTool.checkNewFileName(MainActivity.this, v)) {
                    binder.save(v);
                    unbindService(connection);
                    saveDialog.dismiss();
                }
            }
        });
        saveBinding.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.delete();
                unbindService(connection);
                saveDialog.dismiss();
            }
        });
        saveBinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNeedResumeWhenBack && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binder.resume();
                }
                saveDialog.dismiss();
            }
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state", state);
    }

    public void about(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void showFileList(View view) {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_SHOW_FILE, new RequestPermissionsCallBack() {
            @Override
            public void onGranted() {
                startActivity(new Intent(MainActivity.this, FileListActivity.class));
            }
        });
    }

    public void setting(View view) {
        startActivity(new Intent(this, SettingActivity.class));
    }
}