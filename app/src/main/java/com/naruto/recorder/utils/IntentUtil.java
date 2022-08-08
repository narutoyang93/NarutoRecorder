package com.naruto.recorder.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/4 0004
 * @Note
 */
public class IntentUtil {

    public static Intent createBroadcastIntent(Context context, String action, Class<?> cls) {
        Intent intent = new Intent(action);
        if (cls != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上的静态广播需要指定component
            intent.setComponent(new ComponentName(context, cls));
        }
        Log.i("naruto", "--->create action " + action);
        return intent;
    }

    public static int createPendingIntentFlag(int flag) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                (PendingIntent.FLAG_IMMUTABLE | flag) : flag;
    }

    public static int defaultPendingIntentFlag() {
        return createPendingIntentFlag(PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
