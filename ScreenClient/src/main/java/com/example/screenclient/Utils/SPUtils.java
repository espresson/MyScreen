package com.example.screenclient.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @Author hzh
 * E-Mail Address：565150953@qq.com
 * @Date 10:58
 * @Description
 */
public class SPUtils {
    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
    }


    public static void removeObject(Context context,Class<?> clazz){
        remove(context,getKey(clazz));
    }

    public static String getKey(Class<?> clazz) {
        return clazz.getName();
    }

    public static String getKey(Type type) {
        return type.toString();
    }

    public static void remove(Context context,String key) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.remove(key);
        edit.commit();
    }

    public static void putBoolean(Context context,String key, boolean value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean(key, value);
        edit.commit();
    }

    public static boolean getBoolean(Context context,String key, boolean defValue) {
        return getPreferences(context).getBoolean(key, defValue);
    }

    public static void putString(Context context,String key, String value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString(key, value);
        edit.commit();
    }

    public static String getString(Context context,String key, String defValue) {
        return getPreferences(context).getString(key, defValue);
    }

    public static void putInt(Context context,String key, int value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt(key, value);
        edit.commit();
    }

    public static int getInt(Context context,String key, int defValue) {
        return getPreferences(context).getInt(key, defValue);
    }

    public static void putLong(Context context,String key, long value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putLong(key, value);
        edit.commit();
    }

    public static long getLong(Context context,String key, long defValue) {
        return getPreferences(context).getLong(key, defValue);
    }

    public static void putFloat(Context context,String key, float value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putFloat(key, value);
        edit.commit();
    }

    public static float getFloat(Context context,String key, float defValue) {
        return getPreferences(context).getFloat(key, defValue);
    }

    public static void putStringSet(Context context,String key, Set<String> value) {
        SharedPreferences sp = getPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putStringSet(key, value);
        edit.commit();
    }

    public static Set<String> getStringSet(Context context,String key, Set<String> defValue) {
        return getPreferences(context).getStringSet(key, defValue);
    }
}
