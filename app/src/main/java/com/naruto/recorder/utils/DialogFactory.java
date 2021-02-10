package com.naruto.recorder.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.naruto.recorder.R;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;


/**
 * @Purpose 弹窗工具
 * @Author Naruto Yang
 * @CreateDate 2020/4/01 0001
 * @Note
 */
public class DialogFactory {

    public static AlertDialog showHintDialog(int messageResId, Activity activity) {
        return showHintDialog(messageResId, null, activity, null);
    }

    public static AlertDialog showHintDialog(String message, Activity activity) {
        return showHintDialog(-1, message, activity, null);
    }

    /**
     * 弹窗提示信息
     *
     * @param messageResId
     * @param message
     * @param activity
     * @return
     */
    public static AlertDialog showHintDialog(int messageResId, String message, Activity activity, View.OnClickListener onConfirmListener) {
        DialogData dialogData = new DialogData();
        dialogData.layoutResId = R.layout.dialog_hint;
        dialogData.contentResId = messageResId;
        dialogData.content = message;
        dialogData.confirmListener = onConfirmListener;
        AlertDialog dialog = makeSimpleDialog(activity, dialogData).first;
        dialog.show();
        return dialog;
    }


    /***
     * 加载弹窗
     * @param activity
     * @return
     */
    public static AlertDialog showLoadingDialog(Activity activity) {
        DialogData dialogData = new DialogData();
        dialogData.layoutResId = R.layout.dialog_loading;
        dialogData.themeResId = 0;
        AlertDialog dialog = makeSimpleDialog(activity, dialogData).first;
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }


    /**
     * 创建简单弹窗
     *
     * @param context
     * @param dialogData
     * @return
     */
    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, DialogData dialogData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogData.themeResId);
        View view = LayoutInflater.from(context).inflate(dialogData.layoutResId, null);
        final AlertDialog dialog = builder.setView(view).create();
        //设置文本
        setText(view, R.id.tv_title, dialogData.title);
        if (dialogData.isNeedCancelBtn) {
            setText(view, R.id.btn_cancel, dialogData.cancelText);
        } else {
            View cancelBtn = view.findViewById(R.id.btn_cancel);
            if (cancelBtn != null)
                cancelBtn.setVisibility(View.GONE);
        }
        setText(view, R.id.btn_confirm, dialogData.confirmText);
        if (dialogData.contentResId != -1) {
            ((TextView) view.findViewById(R.id.tv_content)).setText(dialogData.contentResId);
        } else {
            setText(view, R.id.tv_content, dialogData.content);
        }

        //设置点击事件
        setOnClickListener(dialog, view, R.id.btn_cancel, dialogData.cancelListener);
        setOnClickListener(dialog, view, R.id.btn_confirm, dialogData.confirmListener);
        return new Pair<>(dialog, view);
    }

    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, String title, String messageContent, View.OnClickListener confirmListener, View.OnClickListener cancelListener) {
        DialogData dialogData = new DialogData();
        dialogData.title = title;
        dialogData.content = messageContent;
        dialogData.confirmListener = confirmListener;
        dialogData.cancelListener = cancelListener;
        return makeSimpleDialog(context, dialogData);
    }

    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, String title, String messageContent, String confirmText, View.OnClickListener confirmListener) {
        DialogData dialogData = new DialogData();
        dialogData.title = title;
        dialogData.content = messageContent;
        dialogData.confirmText = confirmText;
        dialogData.confirmListener = confirmListener;
        return makeSimpleDialog(context, dialogData);
    }

    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, String title, String messageContent, View.OnClickListener confirmListener) {
        return makeSimpleDialog(context, title, messageContent, confirmListener, null);
    }

    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, String messageContent, View.OnClickListener confirmListener) {
        return makeSimpleDialog(context, null, messageContent, confirmListener);
    }


    public static Pair<AlertDialog, View> makeSimpleDialog(Context context, @LayoutRes int layoutRes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DefaultDialogStyle);
        View view = LayoutInflater.from(context).inflate(layoutRes, null);
        final AlertDialog dialog = builder.setView(view).create();
        setOnClickListener(dialog, view, R.id.btn_confirm, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return new Pair<>(dialog, view);
    }


    /**
     * 设置文本
     *
     * @param dialogView
     * @param textViewId
     * @param text
     */
    private static void setText(View dialogView, int textViewId, String text) {
        TextView textView = dialogView.findViewById(textViewId);
        if (textView != null && !TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 设置点击监听
     *
     * @param dialog
     * @param dialogView
     * @param viewId
     * @param onClickListener
     */
    private static void setOnClickListener(final AlertDialog dialog, View dialogView, int viewId, final View.OnClickListener onClickListener) {
        View view = dialogView.findViewById(viewId);
        if (view == null) return;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (onClickListener != null) {
                    onClickListener.onClick(v);
                }
            }
        });
    }

    /**
     * @Purpose 弹窗所需配置的数据
     * @Author Naruto Yang
     * @CreateDate 2020/4/01 0001
     * @Note
     */
    public static class DialogData {
        public int themeResId = R.style.DefaultDialogStyle;
        public int layoutResId = R.layout.dialog_simple_2_button;//布局id
        public String title;//标题
        public String content;//内容
        public String cancelText;//取消按钮文本
        public String confirmText;//确定按钮文本
        public int contentResId = -1;//内容文本资源id
        public View.OnClickListener cancelListener;
        public View.OnClickListener confirmListener;
        public boolean isNeedCancelBtn = true;
    }

    /**
     * @Purpose 菜单弹窗选中回调
     * @Author Naruto Yang
     * @CreateDate 2020/4/16 0016
     * @Note
     */
    public interface OnSelectedDataCallback {
        void onSelected(String text, Object value);
    }
}
