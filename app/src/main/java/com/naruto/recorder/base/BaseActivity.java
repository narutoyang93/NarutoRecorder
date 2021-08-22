package com.naruto.recorder.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.naruto.recorder.MyApplication;
import com.naruto.recorder.R;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.statusbar.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        MyApplication.toast(message);
    }

    /**
     * 显示软键盘
     */
    public void showSoftKeyboard(EditText editText) {
        editText.post(() -> {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
        });
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 监听软键盘显示或隐藏
     *
     * @param listener
     */
    protected void setSoftKeyboardListener(final SoftKeyboardListener listener) {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Rect r = new Rect();
                                rootView.getWindowVisibleDisplayFrame(r);
                                int screenHeight = rootView.getHeight();
                                int keyboardHeight = screenHeight - (r.bottom);
                                if (keyboardHeight > 200) {
                                    listener.onShow(keyboardHeight);//软键盘显示
                                } else {
                                    listener.onHide(); //软键盘隐藏
                                }
                            }

                        });
            }
        });
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

    public void startActivity(Class<? extends Activity> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    /**
     * 检查并申请权限
     *
     * @param callBack
     */
    public void doWithPermission(RequestPermissionsCallBack callBack) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {//6.0以下系统无需动态申请权限
            if (callBack != null) callBack.onGranted();
            return;
        }
        //检查权限
        List<String> requestPermissionsList = checkPermissions(callBack.permissions);//记录需要申请的权限
        if (requestPermissionsList.isEmpty()) {//均已授权
            callBack.onGranted();
        } else {//申请
            String[] requestPermissionsArray = requestPermissionsList.toArray(new String[requestPermissionsList.size()]);
            requestPermissions(requestPermissionsArray, result -> {
                        List<String> refuseList = new ArrayList<>();//被拒绝的权限
                        List<String> shouldShowReasonList = new ArrayList<>();//需要提示申请理由的权限，即没有被设置“不再询问”的权限
                        String permission;
                        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                            if (entry.getValue()) continue;
                            refuseList.add(permission = entry.getKey());
                            if (shouldShowRequestPermissionRationale(permission))
                                shouldShowReasonList.add(permission);
                        }
                        if (refuseList.isEmpty()) {//全部已授权
                            callBack.onGranted();
                        } else {//被拒绝
                            if (TextUtils.isEmpty(callBack.requestPermissionReason))
                                callBack.onDenied(this);//直接执行拒绝后的操作
                            else {//弹窗
                                if (shouldShowReasonList.isEmpty()) //被设置“不再询问”
                                    showGoToSettingPermissionDialog(callBack);//弹窗引导前往设置页面
                                else
                                    showPermissionRequestReasonDialog(callBack);//弹窗显示申请原因并重新请求权限
                            }
                        }
                    }
            );
        }
    }


    /**
     * startActivityForResult
     *
     * @param intent
     * @param callback
     */
    public void startActivityForResult(Intent intent, ActivityResultCallback<ActivityResult> callback) {
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback).launch(intent);
    }

    public void requestPermissions(String[] permissions, ActivityResultCallback<Map<String, Boolean>> callback) {
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), callback).launch(permissions);
    }


    /**
     * 显示引导设置权限弹窗
     *
     * @param callback
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public AlertDialog showGoToSettingPermissionDialog(RequestPermissionsCallBack callback) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        AlertDialog permissionDialog = DialogFactory.makeGoSettingDialog(this
                , callback.requestPermissionReason + "，是否前往设置？", intent
                , () -> callback.onDenied(this)
                , result -> {
                    if (checkPermissions(callback.permissions).isEmpty()) {//已获取权限
                        callback.onGranted();
                    } else {//被拒绝
                        callback.onDenied(BaseActivity.this);
                    }
                }).first;
        permissionDialog.show();
        return permissionDialog;
    }

    /**
     * 显示申请权限理由
     *
     * @param callback
     * @return
     */
    public AlertDialog showPermissionRequestReasonDialog(RequestPermissionsCallBack callback) {
        DialogFactory.DialogData dialogData = new DialogFactory.DialogData();
        dialogData.title = "提示";
        dialogData.content = callback.requestPermissionReason;
        dialogData.cancelText = "取消";
        dialogData.confirmText = "授予";
        dialogData.cancelListener = v -> callback.onDenied(this);
        dialogData.confirmListener = v -> doWithPermission(callback);
        AlertDialog dialog = DialogFactory.makeSimpleDialog(this, dialogData).first;
        dialog.show();
        return dialog;
    }


    /**
     * 检查权限
     *
     * @param permissions
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected List<String> checkPermissions(String... permissions) {
        List<String> list = new ArrayList<>();
        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {//未授权，记录下来
                list.add(p);
            }
        }
        return list;
    }

    /**
     * 启动其他页面
     *
     * @param activityClass
     */
    protected void launchActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
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
     * @Purpose 键盘收展监听
     * @Author Naruto Yang
     * @CreateDate 2020/5/09 0009
     * @Note
     */
    protected interface SoftKeyboardListener {
        /**
         * 弹出
         */
        void onShow(int keyboardHeight);

        /**
         * 收起
         */
        void onHide();
    }


    /**
     * @Purpose 申请权限后处理接口
     * @Author Naruto Yang
     * @CreateDate 2019/12/19
     * @Note
     */
    public static abstract class RequestPermissionsCallBack {
        public String[] permissions;
        public String requestPermissionReason;

        public RequestPermissionsCallBack(String requestPermissionReason, String... permissions) {
            this.permissions = permissions;
            this.requestPermissionReason = requestPermissionReason;
        }

        public RequestPermissionsCallBack(String[] permissions) {
            this(null, permissions);
        }

        /**
         * 已授权
         */
        public abstract void onGranted();

        /**
         * 被拒绝
         *
         * @param context
         */
        public void onDenied(Context context) {
            Toast.makeText(context, "授权失败", Toast.LENGTH_SHORT).show();
        }

    }
}
