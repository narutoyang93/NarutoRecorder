package com.naruto.recorder.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
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
     * 根据文件获取文件uri
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
     * 获取要分享的文件的uri（用这种方式返回的Uri才能被外部应用识别）
     *
     * @param context
     * @param file
     * @return
     */
    private static Uri getFileUriForShare(Context context, File file) {
        String volumeName = "external";
        String filePath = file.getAbsolutePath();
        String[] projection = new String[]{MediaStore.Files.FileColumns._ID};
        Uri uri = null;

        Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri(volumeName), projection,
                MediaStore.MediaColumns.DATA + "=? ", new String[]{filePath}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                uri = MediaStore.Files.getContentUri(volumeName, id);
            }
            cursor.close();
        }

        return uri;
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
        for (File f : files) {
            uris.add(getFileUriForShare(activity, f));
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
     * 获取编译时间
     *
     * @param format 日期格式
     * @return
     */
    public static String getBuildTime(String format) {
        return new SimpleDateFormat(format).format(new Date(BuildConfig.TIMESTAMP));
    }

    /**
     * 重命名文件时检查文件名
     *
     * @param context
     * @param fileName
     * @return
     */
    public static boolean checkNewFileName(Context context, String fileName) {
        if (fileName.length() == 0) {
            Toast.makeText(context, "未输入文件名", Toast.LENGTH_SHORT).show();
        }
        String s = "/\\:*?\"<>|";
        for (int i = 0; i < s.length(); i++) {
            if (fileName.indexOf(s.charAt(i)) != -1) {
                Toast.makeText(context, String.format("文件名不能包含%s等符号", s), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }


    /**
     * 创建含输入框的底部dialog（不会被键盘遮挡）
     *
     * @param context
     * @param contentView
     * @return
     */
    public static Dialog createBottomInputDialog(Context context, View contentView) {
        return createBottomDialog(context, R.style.dialog_soft_input, contentView);
    }

    public static Dialog createBottomDialog(Context context, @StyleRes int themeResId, View contentView) {
        if (themeResId == 0) themeResId = R.style.dialog_transparent;
        Dialog dialog = new Dialog(context, themeResId);
        dialog.setContentView(contentView);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.getDecorView().setPadding(0, 0, 0, context.getResources().getDimensionPixelSize(R.dimen.dp_10));
        return dialog;
    }

}
