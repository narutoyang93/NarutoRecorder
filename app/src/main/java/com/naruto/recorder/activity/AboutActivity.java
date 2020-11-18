package com.naruto.recorder.activity;

import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.naruto.recorder.InterfaceFactory;
import com.naruto.recorder.R;
import com.naruto.recorder.base.BaseActivity;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.MyTool;

/**
 * @Purpose 关于
 * @Author Naruto Yang
 * @CreateDate 2020/11/15 0015
 * @Note
 */
public class AboutActivity extends BaseActivity {
    private long lastClickIconTime = 0;//上次点击图标的时间
    private int clickIconTimes;//连续点击图标次数
    private static final int VALID_CONTINUOUS_CLICKING_TIMES = 6;//有效的连续点击次数

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_about;
    }

    @Override
    protected void init() {
        setTitleBarTitle("关于");
        ((TextView) findViewById(R.id.tv_time)).setText(MyTool.getBuildTime("yyyy.MM.dd"));
        InterfaceFactory.SimpleOperation operator1 = () -> toast("服务条款");

        InterfaceFactory.SimpleOperation operator2 = () -> toast("隐私声明");

        MyTool.setClickSpannableString(findViewById(R.id.tv_protocol), getString(R.string.protocol)
                , new Pair<>("《服务条款》", operator1), new Pair<>("《隐私声明》", operator2));
    }

    public void showDeveloperInfo(View view) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickIconTime < 2000) {//2秒内连续点击图标触发连击次数计算
            clickIconTimes++;
            if (clickIconTimes == VALID_CONTINUOUS_CLICKING_TIMES) {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                String message = String.format("当前设备dpi:%d，基准比例:%f，屏幕宽度%d", dm.densityDpi, dm.density, (int) (dm.widthPixels / dm.density));
                DialogFactory.showHintDialog(message, this).setCancelable(false);
                clickIconTimes = 1;
                lastClickIconTime = 0;
            } else {
                toast(String.format("再按%d次显示开发信息", VALID_CONTINUOUS_CLICKING_TIMES - clickIconTimes));
            }
        } else {
            clickIconTimes = 1;
        }
        lastClickIconTime = currentTime;
    }
}
