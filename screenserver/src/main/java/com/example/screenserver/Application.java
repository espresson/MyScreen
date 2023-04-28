package com.example.screenserver;

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
    }
}
