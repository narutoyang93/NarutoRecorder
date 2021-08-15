package com.naruto.recorder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.naruto.recorder.base.BaseActivity;
import com.naruto.recorder.helper.ToastHelper;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;


/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2019/8/26 0026
 * @Note
 */
public class MyApplication extends Application {
    private static Context context;
    private static ToastHelper toastHelper;
    private static WeakReference<Activity> currentActivity;//当前正在活动的Activity

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        //监听activity生命周期
        registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());
    }

    public static Context getContext() {
        return context;
    }

    /**
     * 获取dimen资源
     *
     * @param dimenId
     * @return
     */
    public static int getDimension(@DimenRes int dimenId) {
        return context.getResources().getDimensionPixelSize(dimenId);
    }

    /**
     * @param message
     */
    public static void toast(String message) {
        if (toastHelper == null) toastHelper = new ToastHelper(context);
        toastHelper.show(message);
    }

    /**
     * 利用当前活动的Activity执行操作
     *
     * @param operation
     * @return Activity的hashCode
     */
    public static int doByActivity(InterfaceFactory.Operation<BaseActivity> operation) {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null) return -1;
        operation.done((BaseActivity) activity);
        return activity.hashCode();
    }

    /**
     * 执行需要权限的操作
     *
     * @param callBack
     */
    public static void doWithPermission(BaseActivity.RequestPermissionsCallBack callBack) {
        doByActivity(activity -> activity.doWithPermission(callBack));
    }

    /**
     * 显示加载弹窗
     */
    public static int showProgressDialog() {
        return doByActivity(activity -> activity.showLoadingDialog());
    }

    /**
     * @param activityHashCode
     */
    public static void dismissProgressDialog(int activityHashCode) {
        doByActivity(activity -> {
            if (activity.hashCode() == activityHashCode)
                activity.dismissLoadingDialog();
        });
    }

    /**
     * @Description Lifecycle回调
     * @Author Naruto Yang
     * @CreateDate 2021/5/10 0010
     * @Note
     */
    private static class MyActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(@NonNull @NotNull Activity activity, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(@NonNull @NotNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull @NotNull Activity activity) {
            currentActivity = new WeakReference<>(activity);//记录当前正在活动的activity
        }

        @Override
        public void onActivityPaused(@NonNull @NotNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull @NotNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull @NotNull Activity activity, @NonNull @NotNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull @NotNull Activity activity) {

        }
    }
}
