package com.naruto.recorder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.naruto.recorder.utils.MyTool;

/**
 * @Purpose 底部输入框弹窗，用于解决键盘遮挡底部输入框
 * @Author Naruto Yang
 * @CreateDate 2020/5/20 0020
 * @Note
 */
public class EditorDialog extends Dialog {
    private Context context;
    private View contentView;
    private EditText editText;
    private EditText bindingEditText;//所绑定的editText，即所关联EditText（当弹窗dismiss时将文本同步到bindingEditText）

    public EditorDialog(Context context, View contentView, EditText editText, EditText bindingEditText) {
        this(context, R.style.dialog_soft_input, contentView, editText, bindingEditText);
    }

    public EditorDialog(Context context, int themeResId, View contentView, EditText editText, EditText bindingEditText) {
        super(context, themeResId);
        this.context = context;
        this.contentView = contentView;
        this.editText = editText;
        this.bindingEditText = bindingEditText;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView);
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.horizontalMargin = 0f;
        window.setAttributes(layoutParams);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.getDecorView().setPadding(0, 0, 0, 0);
        window.setGravity(Gravity.BOTTOM);


        if (bindingEditText != null) {
            setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    bindingEditText.setText(editText.getText());
                }
            });
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    int screenHeight = MyTool.getDisplayHeight(context);
                    boolean flag = false;//标记键盘已弹出

                    @Override
                    public void onGlobalLayout() {
                        int[] location = new int[2];
                        contentView.getLocationOnScreen(location);
                        int heightDifference = screenHeight - (location[1] + contentView.getHeight());
                        if (heightDifference > 200) {//键盘已弹出
                            flag = true;
                        } else {//键盘已收起
                            if (flag) {
                                flag = false;
                                dismiss();
                            }
                        }
                    }

                });
    }

    public EditText getEditText() {
        return editText;
    }
}
