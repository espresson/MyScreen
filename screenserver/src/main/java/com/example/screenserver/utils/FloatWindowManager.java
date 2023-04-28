package com.example.screenserver.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;


import com.example.screenserver.Application;
import com.example.screenserver.R;
import com.example.screenserver.bean.OperationModel;

import java.util.Date;

/**
 * Author: HuangYuGuang
 * Date: 2022/12/3
 */
public class FloatWindowManager {
    private final String TAG = FloatWindowManager.class.getSimpleName();

    private WindowManager mWindowManager;
    private OperationModel operationModel;
    private long dateStart, dateEnd, during;
    private ViewGroup floatPriview;

    private OnGestureCallback onGestureCallback;

    private static FloatWindowManager instance;

    public static  FloatWindowManager getInstance(){
        if(instance == null){
            synchronized (FloatWindowManager.class){
                if(instance == null){
                    instance = new FloatWindowManager();
                }
            }
        }
        return instance;
    }


    /**
     * 初始化悬浮窗的基本参数（位置、宽高等）
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initWindow(OnGestureCallback onGestureCallback) {
        if(floatPriview != null) {
            floatPriview.setVisibility(View.VISIBLE);
            return;
        }
        if (!Settings.canDrawOverlays(Application.getmApp())) {
            ToastUtils.show("需要开启悬浮窗权限！");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Application.getmApp().startActivity(intent);
            return;
        }
        this.onGestureCallback = onGestureCallback;
        //得到容器，通过这个inflater来获得悬浮窗控件
        LayoutInflater inflater = LayoutInflater.from(Application.getmApp());
        // 获取浮动窗口视图所在布局
        View mFloatingLayout = inflater.inflate(R.layout.window_float_view, null);
        floatPriview = mFloatingLayout.findViewById(R.id.float_view); //悬浮容器父布局
        mWindowManager = (WindowManager) Application.getmApp().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = getParams();//设置好悬浮窗的参数
        wmParams.format = PixelFormat.RGBA_8888; //设置透明背景色
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        //悬浮窗的开始位置（0，0）
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = 1920;
        wmParams.height = 1080;
        try {
            // 添加悬浮窗的视图
            mWindowManager.addView(mFloatingLayout, wmParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        floatPriview.setOnTouchListener(onTouchListener);
    }


    private WindowManager.LayoutParams getParams() {
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //  wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        wmParams.type = getWindowManagerType();
        //设置可以显示在状态栏上
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        return wmParams;
    }

    /**
     * 获取 窗口类型，版本适配
     *
     * @return
     */
    private int getWindowManagerType() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {//4.4 以下
            return WindowManager.LayoutParams.TYPE_PHONE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) { //4.4--7.0
            return WindowManager.LayoutParams.TYPE_TOAST;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {//7.11
            return WindowManager.LayoutParams.TYPE_PHONE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_TOAST;
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Date date = new Date();
            if (view.getId() == R.id.float_view) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_DOWN = " + motionEvent.getAction());
                        LogUtils.d(TAG, "onTouchEvent: getX:" + motionEvent.getX());
                        LogUtils.d(TAG, "onTouchEvent: getY:" + motionEvent.getY());
                        if (operationModel == null) {
                            operationModel = new OperationModel();
                        }
                        dateStart = date.getTime();
                        operationModel.setDownPoint(new Float[]{motionEvent.getX(), motionEvent.getY()});
                        break;
                    case MotionEvent.ACTION_MOVE:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_MOVE = " + motionEvent.getAction());
                        LogUtils.d(TAG, "onTouchEvent: getX:" + motionEvent.getX());
                        LogUtils.d(TAG, "onTouchEvent: getY:" + motionEvent.getY());
                        operationModel.addLocationModel(new Float[]{motionEvent.getX(), motionEvent.getY()});
                        break;
                    case MotionEvent.ACTION_UP:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_UP = " + motionEvent.getAction());
                        LogUtils.d(TAG, "onTouchEvent: getX:" + motionEvent.getX());
                        LogUtils.d(TAG, "onTouchEvent: getY:" + motionEvent.getY());
                        dateEnd = date.getTime();
                        during = dateEnd - dateStart;
                        operationModel.addLocationModel(new Float[]{motionEvent.getX(), motionEvent.getY()});
                        operationModel.setDelayTime(0);
                        operationModel.setDurationTime((int) during);
                        LogUtils.d(TAG, "onTouch: during:" + during);
                        if(onGestureCallback != null){
                            onGestureCallback.onGesture(operationModel);
                        }
                        break;
                }
            }
            return false;
        }
    };

    public void showFloatWindow(boolean isShow){
        if(floatPriview != null) floatPriview.setVisibility(isShow?View.VISIBLE:View.GONE);
    }

    public void setOnGestureCallback(OnGestureCallback onGestureCallback) {
        this.onGestureCallback = onGestureCallback;
    }

    public interface OnGestureCallback{
        void onGesture(OperationModel operationModel);
    }
}
