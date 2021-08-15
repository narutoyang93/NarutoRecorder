package com.naruto.recorder.helper;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/6/17 0017
 * @Note
 */
public class ToastHelper {
    private Context context;
    private Toast toast;
    private boolean isToastShowing = false;

    public ToastHelper(Context context) {
        this.context = context;
    }

    /**
     * @param message
     */
    public void show(String message) {
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                toast.addCallback(new Toast.Callback() {
                    @Override
                    public void onToastShown() {
                        super.onToastShown();
                        isToastShowing = true;
                    }

                    @Override
                    public void onToastHidden() {
                        super.onToastHidden();
                        isToastShowing = false;
                    }
                });
            }
        } else {
            boolean isShowing;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) isShowing = isToastShowing;
            else isShowing = toast.getView().getWindowVisibility() == View.VISIBLE;

            if (isShowing) {
                toast.cancel();
                toast = null;
                show(message);
                return;
            }
            toast.setText(message);
        }
        toast.show();
    }

}
