package com.naruto.recorder.utils;


import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

/**
 * @Purpose DataBinding工具类
 * @Author Naruto Yang
 * @CreateDate 2019/9/12 0012
 * @Note
 */
public class DataBindingUtils {

    @BindingAdapter("textChangedListener")
    public static void setTextChangedListener(EditText editText, TextWatcher watcher) {
        editText.addTextChangedListener(watcher);
    }


    @BindingAdapter("htmlText")
    /**
     * 设置html文本（可用于让局部文字颜色高亮）
     *
     * @param textView
     * @param text
     */
    public static void htmlText(TextView textView, String text) {
        Log.d("DataBindingUtils", "--->htmlText: ");
        textView.setText(Html.fromHtml(text));
    }

    @BindingAdapter("setSelected")
    /**
     *
     */
    public static void setSelected(View view, boolean selected) {
        view.setSelected(selected);
    }


    /**
     * @param value
     * @return
     */
    public static int isVisible(boolean value) {
        return value ? View.VISIBLE : View.GONE;
    }


    /**
     * @param value
     * @return
     */
    public static int isVisible2(boolean value) {
        return value ? View.VISIBLE : View.INVISIBLE;
    }

    @BindingAdapter("inhibitInputEmoticon")
    /**
     * 禁止输入表情
     */
    public static void inhibitInputEmoticon(EditText editText, Object o) {
        MyTool.inhibitInputEmoticon(editText);
    }
}
