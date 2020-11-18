package com.naruto.recorder.utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.naruto.recorder.BuildConfig;
import com.naruto.recorder.InterfaceFactory;
import com.naruto.recorder.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/2/13 0013
 * @Note
 */
public class MyTool {

    /**
     * 设置文本部分点击监听
     *
     * @param textView
     * @param text
     * @param targets
     */
    public static void setClickSpannableString(TextView textView, String text, Pair<String, InterfaceFactory.SimpleOperation>... targets) {
        if (targets.length > 0) {
            SpannableString ss = new SpannableString(text);
            int start, end;
            int color = ContextCompat.getColor(textView.getContext(), R.color.theme);
            ClickableSpan clickableSpan;
            for (Pair<String, InterfaceFactory.SimpleOperation> pair : targets) {
                start = text.indexOf(pair.first);
                if (start < 0) continue;
                clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        pair.second.done();
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(color);//设置文本颜色
                        ds.setUnderlineText(false);//去掉下划线
                    }
                };
                ss.setSpan(clickableSpan, start, end = start + pair.first.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(ss);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setHighlightColor(Color.parseColor("#00000000"));//防止点击后文本背景高亮
        }
    }

    /**
     * 格式化时间日期
     *
     * @param milliseconds
     * @param formatType
     * @return
     */
    public static String formatTime(Long milliseconds, String formatType) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(formatType);
        cal.setTimeInMillis(milliseconds);
        return formatter.format(cal.getTime());
    }

    /**
     * 根据毫秒换算时间
     *
     * @param milliseconds
     * @return
     */
    public static String getTimeString(int milliseconds) {
        int temp;
        int s = (temp = milliseconds / 1000) % 60;//秒
        int m = (temp /= 60) % 60;//分
        int h = temp / 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * 禁止输入框输入表情
     *
     * @param editText
     */
    public static void inhibitInputEmoticon(EditText editText) {
        editText.setFilters(new InputFilter[]{new InputFilter() {
            Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Matcher emojiMatcher = emoji.matcher(source);
                if (emojiMatcher.find()) {
                    Toast.makeText(editText.getContext(), "不支持输入表情", Toast.LENGTH_SHORT).show();
                    return "";
                }
                return null;
            }
        }});
    }

    /**
     * 获取屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getDisplayWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @param context
     * @return
     */
    public static int getDisplayHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    /**
     * 根据获取文件uri
     *
     * @param context
     * @param file
     * @return
     */
    public static Uri getFileUri(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    /**
     * 分享文件
     *
     * @param activity
     * @param files
     */
    public static void shareFile(Activity activity, List<File> files) {
        Intent intent = new Intent();
        //获取文件uri
        ArrayList<Uri> uris = new ArrayList<>();
        Uri uri = null;
        for (File f : files) {
            uris.add(MyTool.getFileUri(activity, f));
        }

        if (uris.size() == 1) {
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {//批量分享
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setType("*/*");
        intent = Intent.createChooser(intent, "Here is the title of Select box");
        activity.startActivity(intent);
    }


    /**
     * 设置Service为前台服务
     *
     * @param service
     * @param activityClass  点击通知将会打开的Activity
     * @param iconRes        通知图标
     * @param notificationId
     */
    public static NotificationCompat.Builder setForegroundService(Service service, Class<? extends Activity> activityClass, @DrawableRes int iconRes, int notificationId) {
        final String CHANNEL_ID = service.getPackageName() + ".notification.channel";
        Intent intent = new Intent(service, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //创建通知渠道
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, service.getString(R.string.app_name), importance);
            channel.setDescription("渠道描述");
            channel.setSound(null, null);
            NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
        }
        //创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(iconRes)
                .setWhen(System.currentTimeMillis());
  /*              .setContentTitle("这是测试通知标题")  //设置标题
                .setContentText("这是测试通知内容") //设置内容*/
        service.startForeground(notificationId, builder.build());

        return builder;
    }


    /**
     * 获取编译时间
     * @param format 日期格式
     * @return
     */
    public static String getBuildTime(String format){
        return new SimpleDateFormat(format).format(new Date(BuildConfig.TIMESTAMP));
    }


}