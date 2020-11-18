package com.naruto.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;


/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2019/6/5
 * @Note
 */
public class SharedPreferencesHelper {
    public static final String KEY_SORT_TYPE = "sort_type";
    public static final String KEY_IS_DESCENDING_ORDER = "is_descending_order";//是否降序

    /**
     * 设置排序方式
     *
     * @param sortType          排序类型："title"/"time"
     * @param isDescendingOrder 是否降序
     */
    public static void setSortType(String sortType, boolean isDescendingOrder) {
        setStringValue(KEY_SORT_TYPE, sortType);
        setBooleanValue(KEY_IS_DESCENDING_ORDER, isDescendingOrder);
    }

    /**
     * 获取排序方式
     *
     * @return
     */
    public static Pair<String, Boolean> getSortType() {
        String type = getSharedPreferences().getString(KEY_SORT_TYPE, "time");
        boolean isdescendingOrder = getSharedPreferences().getBoolean(KEY_IS_DESCENDING_ORDER, true);
        return new Pair<>(type, isdescendingOrder);
    }

    /**
     * 清空所有数据
     */
    public static void clear() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.clear();
        editor.commit();
    }


////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //以下为内部方法


    private static SharedPreferences getSharedPreferences() {
        Context context = MyApplication.getContext();
        final String SP_NAME = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        return MyApplication.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private static void setStringValue(final String key, final String value) {
        setValue(new SetValueHelper() {
            @Override
            public void setValue(SharedPreferences.Editor editor) {
                editor.putString(key, value);
            }
        });
    }

    private static void setBooleanValue(final String key, final boolean value) {
        setValue(new SetValueHelper() {
            @Override
            public void setValue(SharedPreferences.Editor editor) {
                editor.putBoolean(key, value);
            }
        });
    }

    private static void setFloatValue(final String key, final float value) {
        setValue(new SetValueHelper() {
            @Override
            public void setValue(SharedPreferences.Editor editor) {
                editor.putFloat(key, value);
            }
        });
    }

    private static void setIntValue(final String key, final int value) {
        setValue(new SetValueHelper() {
            @Override
            public void setValue(SharedPreferences.Editor editor) {
                editor.putInt(key, value);
            }
        });
    }

    private static void setLongValue(final String key, final long value) {
        setValue(new SetValueHelper() {
            @Override
            public void setValue(SharedPreferences.Editor editor) {
                editor.putLong(key, value);
            }
        });
    }


    private static void setValue(SetValueHelper helper) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        helper.setValue(editor);
        editor.commit();
    }


    /**
     * @Purpose
     * @Author Naruto Yang
     * @CreateDate 2020/5/21 0021
     * @Note
     */
    public interface SetValueHelper {
        void setValue(SharedPreferences.Editor editor);
    }
}
