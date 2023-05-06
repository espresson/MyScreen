package com.example.screenserver;


import leakcanary.LeakCanary;

/**
 * @Author hzh
 * E-Mail Address：565150953@qq.com
 * @Date 14:36
 * @Description
 */
public class Application extends android.app.Application {

    private static Application mApp;

    public static Application getmApp() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp=this;
       LeakCanary.getConfig().newBuilder();


    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 在应用退出时手动调用 LeakCanary 的监控方法

    }
}
