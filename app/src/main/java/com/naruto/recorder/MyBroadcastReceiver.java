package com.naruto.recorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.naruto.recorder.utils.IntentUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/4 0004
 * @Note
 */
public class MyBroadcastReceiver extends BroadcastReceiver {
    private boolean isLocalBroadcast;
    private Map<String, InterfaceFactory.Operation<Intent>> operationMap = new HashMap<>();

    public MyBroadcastReceiver(boolean isLocalBroadcast) {
        this.isLocalBroadcast = isLocalBroadcast;
    }

    public MyBroadcastReceiver() {
        this(false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("naruto", "--->onReceive: action=" + intent.getAction());
        InterfaceFactory.Operation<Intent> executor = operationMap.get(intent.getAction());
        if (executor != null) executor.done(intent);
    }

    public void addAction(InterfaceFactory.Operation<Intent> extraOperation, String... systemAction) {
        for (String s : systemAction) {
            operationMap.put(s, extraOperation);
        }
    }

    public void register(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : operationMap.keySet()) {
            intentFilter.addAction(action);
        }

        if (isLocalBroadcast)
            doWithLocalBroadcast(context, (manager) -> manager.registerReceiver(this, intentFilter));
        else context.registerReceiver(this, intentFilter);
    }

    public void unRegister(Context context) {
        if (isLocalBroadcast)
            doWithLocalBroadcast(context, (manager) -> manager.unregisterReceiver(this));
        else context.unregisterReceiver(this);
    }

    /**
     * 发送本地广播
     */
    public static void sendLocalBroadcast(Context context, String action, InterfaceFactory.Operation<Intent> operation) {
        doWithLocalBroadcast(context, (manager) -> {
            Intent intent = IntentUtil.createBroadcastIntent(context, action, MyBroadcastReceiver.class);
            operation.done(intent);
            manager.sendBroadcast(intent);
        });
    }

    public static void doWithLocalBroadcast(Context context, InterfaceFactory.Operation<LocalBroadcastManager> operation) {
        operation.done(LocalBroadcastManager.getInstance(context));
    }
}
