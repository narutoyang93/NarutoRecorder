package com.naruto.recorder.activity;

import com.naruto.recorder.R;
import com.naruto.recorder.base.BaseActivity;

/**
 * @Purpose 设置
 * @Author Naruto Yang
 * @CreateDate 2020/11/15 0015
 * @Note
 */
public class SettingActivity extends BaseActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_setting;
    }

    @Override
    protected void init() {
        setTitleBarTitle("设置");
    }
}