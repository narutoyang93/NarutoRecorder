package com.naruto.recorder.base;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.naruto.recorder.R;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.statusbar.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/13 0013
 * @Note
 */
public abstract class BaseActivity extends AppCompatActivity {
    public AlertDialog loadingDialog;//加载弹窗
    protected View rootView;//根布局，即getLayoutRes()返回的布局
    protected View titleBar;//标题栏
    protected Toast toast;
    private SparseArray<RequestPermissionsCallBack> permissionsCallBackSparseArray = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isScreenRotatable())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 禁用横屏

        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());

        rootView = ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);
        getTitleBar();
        setBackBtnClickListener(null);

        //设置沉浸式状态栏
        if (isNeedSetFitsSystemWindow()) {
            View fitsSystemWindowView = getFitsSystemWindowView();
            if (fitsSystemWindowView != null)
                fitsSystemWindowView.setFitsSystemWindows(true);//设置FitsSystemWindows，使顶部留出状态栏高度的padding
            //设置状态栏透明
            StatusBarUtil.setTranslucentStatus(this);
            //一般的手机的状态栏文字和图标都是白色的, 可如果应用也是纯白色的, 或导致状态栏文字看不清
            if (isNeedSetStatusBarDarkTheme() && !StatusBarUtil.setStatusBarDarkTheme(this, true)) {
                //如果不支持设置深色风格 为了兼容总不能让状态栏白白的看不清, 于是设置一个状态栏颜色为半透明,
                //这样半透明+白=灰, 状态栏的文字能看得清
                StatusBarUtil.setStatusBarColor(this, 0x55000000);
            }
        }

        init();
    }


    /**
     * 是否需要设置状态栏字体颜色为深色
     *
     * @return
     */
    protected boolean isNeedSetStatusBarDarkTheme() {
        return true;
    }

    /**
     * 是否需要设置沉浸式状态栏
     *
     * @return
     */
    protected boolean isNeedSetFitsSystemWindow() {
        return true;
    }

    /**
     * 是否允许旋转屏幕
     *
     * @return
     */
    protected boolean isScreenRotatable() {
        return false;
    }

    /**
     * 设置标题
     *
     * @param title
     * @param backBtnClickListener
     */
    protected void setTitleBarTitle(String title, View.OnClickListener backBtnClickListener) {
        TextView tvTitleBarTitle = findViewById(R.id.tv_titleBar_title);
        if (tvTitleBarTitle != null) {
            tvTitleBarTitle.setText(title);
            tvTitleBarTitle.setSelected(true);//marquee效果需要
        }
        View backBtn = findViewById(R.id.iv_titleBar_back);
        if (backBtn != null) backBtn.setOnClickListener(backBtnClickListener);
    }

    protected void setTitleBarTitle(String title) {
        setTitleBarTitle(title, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 获取标题栏
     */
    protected View getTitleBar() {
        if (titleBar == null)
            titleBar = rootView.findViewWithTag(getString(R.string.title_bar_tag));
        return titleBar;
    }

    /**
     * 获取“沉浸式状态栏”模式下需要顶到状态栏上的view（也就是与状态栏重叠的view）
     *
     * @return
     */
    protected View getFitsSystemWindowView() {
        View fitsSystemWindowView = titleBar == null ? rootView.findViewWithTag(getString(R.string.fits_system_windows_tag)) : titleBar;//需要顶到状态栏位置上的view
        if (fitsSystemWindowView == null) fitsSystemWindowView = rootView;
        return fitsSystemWindowView;
    }

    /**
     * 设置返回按钮点击事件
     *
     * @param clickListener
     */
    protected void setBackBtnClickListener(View.OnClickListener clickListener) {
        View v = titleBar == null ? rootView : titleBar;
        View backBtn = v.findViewWithTag("back_btn");
        if (backBtn != null) {
            if (clickListener == null) {//默认关闭当前页面
                backBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            } else {
                backBtn.setOnClickListener(clickListener);
            }
        }
    }

    protected void showUnimplemented() {
        toast("暂未实现");
    }

    protected void toast(String message) {
        if (toast == null) {
            toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            toast.setText(message);
        }
        toast.show();
    }

    /**
     * 展示等待对话框
     */
    public void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = DialogFactory.showLoadingDialog(this);
        } else if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }


    /**
     * 隐藏等待对话框
     */
    public void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }


    /**
     * 申请权限
     *
     * @param permissions
     * @param callBack
     */
    public void requestPermissions(String[] permissions, int requestCode, RequestPermissionsCallBack callBack) {
        if (Build.VERSION.SDK_INT < 23) {//6.0以下系统无需动态申请权限
            if (callBack != null) callBack.onGranted();
            return;
        }

        List<String> requestPermissionsList = new ArrayList<>();//记录需要申请的权限
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {//未授权，记录下来
                requestPermissionsList.add(p);
            }
        }
        if (requestPermissionsList.isEmpty()) {//已授权
            if (callBack != null) callBack.onGranted();
        } else {//申请
            String[] requestPermissionsArray = requestPermissionsList.toArray(new String[requestPermissionsList.size()]);
            permissionsCallBackSparseArray.put(requestCode, callBack);
            ActivityCompat.requestPermissions(this, requestPermissionsArray, requestCode);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RequestPermissionsCallBack callBack = permissionsCallBackSparseArray.get(requestCode);
        permissionsCallBackSparseArray.remove(requestCode);
        boolean isAllGranted = true;//是否全部已授权
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                isAllGranted = false;
                break;
            }
        }
        if (isAllGranted) {//全部已授权
            if (callBack != null) callBack.onGranted();
        } else {//被拒绝
            if (callBack == null) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                callBack.onDenied(this);
            }
        }
    }

    /**
     * 页面布局文件
     *
     * @return
     */
    protected abstract int getLayoutRes();

    /**
     * 页面初始化
     */
    protected abstract void init();

    /**
     * @Purpose 申请权限后处理接口
     * @Author Naruto Yang
     * @CreateDate 2019/12/19
     * @Note
     */
    public static abstract class RequestPermissionsCallBack {
        /**
         * 已授权
         */
        public abstract void onGranted();

        /**
         * 被拒绝
         */
        public void onDenied(Context context) {
            Toast.makeText(context, "授权失败", Toast.LENGTH_SHORT).show();
        }
    }
}
