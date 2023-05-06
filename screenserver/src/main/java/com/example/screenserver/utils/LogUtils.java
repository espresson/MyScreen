package com.example.screenserver.utils;

import static android.util.Config.DEBUG;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "LogUtils";
    private static int max_log_str_length = 3000;

    private static boolean IS_DEBG = DEBUG;

    public static void i(String msg){
        LogUtils.i(TAG,msg);
    }

    public static void d(String msg){
        LogUtils.d(TAG,msg);
    }

    public static void v(String msg){
        LogUtils.v(TAG,msg);
    }

    public static void e(String msg){
        LogUtils.e(TAG,msg);
    }

    public static void w(String msg){
        LogUtils.w(TAG,msg);
    }

    public static void i(String tag, String msg){
        if(IS_DEBG){
            //大于3000时
            while (msg.length() > max_log_str_length) {
                Log.i(tag, msg.substring(0, max_log_str_length));
                msg = msg.substring(max_log_str_length);
            }
            //剩余部分
            Log.i(tag,msg);
        }
    }

    public static void d(String tag, String msg){
        if(IS_DEBG){
            //大于3000时
            while (msg.length() > max_log_str_length) {
                Log.d(tag, msg.substring(0, max_log_str_length));
                msg = msg.substring(max_log_str_length);
            }
            //剩余部分
            Log.d(tag,msg);
        }
    }

    public static void v(String tag, String msg){
        if(IS_DEBG){
            //大于3000时
            while (msg.length() > max_log_str_length) {
                Log.v(tag, msg.substring(0, max_log_str_length));
                msg = msg.substring(max_log_str_length);
            }
            //剩余部分
            Log.v(tag,msg);
        }
    }

    public static void e(String tag, String msg){
        //大于3000时
        while (msg.length() > max_log_str_length) {
            Log.e(tag, msg.substring(0, max_log_str_length));
            msg = msg.substring(max_log_str_length);
        }
        //剩余部分
        Log.e(tag,msg);
    }

    public static void w(String tag, String msg){
        if(IS_DEBG){
            //大于3000时
            while (msg.length() > max_log_str_length) {
                Log.w(tag, msg.substring(0, max_log_str_length));
                msg = msg.substring(max_log_str_length);
            }
            //剩余部分
            Log.w(tag,msg);
        }
    }

}
