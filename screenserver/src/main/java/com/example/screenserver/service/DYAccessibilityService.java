package com.example.screenserver.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.example.accessibilitytest.utils.AccessibilityUtil;
import com.example.screenserver.MainActivity;
import com.example.screenserver.R;
import com.example.screenserver.StartActivity;
import com.example.screenserver.bean.OperationModel;
import com.example.screenserver.bean.RemoteAssistanceBean;
import com.example.screenserver.utils.FloatWindowManager;
import com.example.screenserver.utils.LogUtils;
import com.example.screenserver.websocket.OnSocketMessage;
import com.example.screenserver.websocket.SocketService;


import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;

import java.util.Calendar;
import java.util.List;

public class DYAccessibilityService extends AccessibilityService {
    private final String TAG = "DYAccessibilityService";

    private int heartBeat1 = 0;
    private int heartBeat2 = 0;

    private SocketService socketService;

    private PowerManager.WakeLock mWakeLock = null;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //这个方法是我们用的最多的方法，我们会在这个方法里写大量的逻辑操作。
        //通过对event的判断执行不同的操作
        //当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
        LogUtils.d(TAG,"onAccessibilityEvent() -- 屏幕发生变化了");
        int eventType = event.getEventType();
        switch (eventType) {
            //通知栏变化时
//            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
//                break;
//            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://当窗口状态发生改变时.
                if("com.android.systemui".equals(event.getPackageName())){
                    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                    if (nodeInfo == null) return;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/button1");
                        for (AccessibilityNodeInfo node : list) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK); //点击允许录屏
                        }
                    }
                }
                break;
        }
        sendScreenImg();
    }

    @Override
    public void onInterrupt() {
        //当服务要被中断时调用.会被调用多次
        LogUtils.d(TAG,"onInterrupt()");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        socketService = SocketService.getInstance();
        socketService.setOnSocketMessage(onSocketMessage);
        socketService.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG," onStartCommand");
        createNotificationChannel();
        getLock();
        startTimer();
        startTimer1();
        //START_STICKY: 被杀后自动重启，保持启动状态，不保持Intent，重新调用onStartCommand，无新Intent则为空Intent—杀死重启后，不继续执行先前任务，能接受新任务
        //START_NOT_STICKY: 被杀后不重启，不保持启动状态，可以随时停止，适合定时数据轮询场景
        //START_REDELIVER_INTENT: 如果有未处理完的Intent，被杀后会重启，并在重启后发送所有Intent。stopSelf后释放保持的Intent。
        return START_STICKY;
    }

    private void createNotificationChannel() {
        //获取一个Notification构造器
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        //点击后跳转的界面，可以设置跳转数据
        Intent nfIntent = new Intent(this, StartActivity.class);

        // 设置PendingIntent
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent,  PendingIntent.FLAG_IMMUTABLE))
                // 设置下拉列表中的图标(大图标)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                // 设置下拉列表里的标题
                //.setContentTitle("SMI InstantView")

                // 设置状态栏内的小图标
                .setSmallIcon(R.mipmap.ic_launcher)
                // 设置上下文内容
                .setContentText("无障碍服务。。。")
                // 设置该通知发生的时间
                .setWhen(System.currentTimeMillis());

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        // 设置该通知发生的时间
        Notification notification = builder.build();
        //设置为默认的声音
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(1, notification);

    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        releaseLock();
        cancelTimer();
        cancelTimer1();
        if (socketService != null){
            socketService.close();
        }
//        if (NettyTcpServer.getInstance().isServerStart()) {
//            NettyTcpServer.getInstance().disconnect();
//        }
        super.onDestroy();
    }

    /**
     * 同步方法   得到休眠锁，保持cpu唤醒
     * 这个API会加剧耗电，所以在非必要情况下尽量不要使用。如果要使用尽量使用最低的等级，并在退出后释放资源。
     */
    synchronized private void getLock(){
        if(mWakeLock==null){
            PowerManager mgr=(PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, DYAccessibilityService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c= Calendar.getInstance();
            c.setTimeInMillis((System.currentTimeMillis()));
            int hour =c.get(Calendar.HOUR_OF_DAY);
            if(hour>=23||hour<=6){
                //当超过timeOut之后系统自动释放WakeLock。
                mWakeLock.acquire(5000);
            }else{
                mWakeLock.acquire(300000);
            }
        }
    }

    synchronized private void releaseLock()
    {
        if(mWakeLock!=null){
            if(mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock=null;
        }
    }

    private OnSocketMessage onSocketMessage = new OnSocketMessage() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onMessage(WebSocket conn, String message) {
            EventBus.getDefault().post(message);
            try {
                if(message.startsWith("RemoteAssistance")){ //远程协助消息
                    RemoteAssistanceBean bean = JSON.parseObject(message.substring(message.indexOf("{")),RemoteAssistanceBean.class);
                    if(bean.getCode() == 2){
                        actionBack();
                    }else if(bean.getCode() == 3){
                        actionHome();
                    }else if(bean.getCode() == 7){
                       // Const.smdtManager.smdtReboot("reboot");
                    }else {
                        OperationModel operationModel = JSON.parseObject(bean.getData(),OperationModel.class);
                        doGestureToScreen(operationModel);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /**
     * 返回键
     */
    private void actionBack(){
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /**
     * HOME键
     */
    private void actionHome(){
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    private void sendScreenImg(){
        /*if(!socketService.isClientConnect()) return;
        if(isCanSend){
            isCanSend = false;
            try{
                Bitmap bitmap = Const.smdtManager.smdtScreenShot(this);
                RemoteAssistanceBean bean = new RemoteAssistanceBean(1, BitmapUtil.bitmapToBase64(bitmap));
                socketService.sendData(JSON.toJSONString(bean));
            }catch (Exception e){
                e.printStackTrace();
            }
            sendHandler.postDelayed(sendRunnable,50);
        }*/
    }

    boolean isCanSend = true; //50ms内只发送一次，避免发送频率太高
    private Handler sendHandler = new Handler();
    private Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            isCanSend = true;
        }
    };

    @TargetApi(Build.VERSION_CODES.N)
    GestureResultCallback callback = new GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);
            FloatWindowManager.getInstance().showFloatWindow(true);
//            LogUtils.d(TAG, "gesture completed");
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            super.onCancelled(gestureDescription);
            FloatWindowManager.getInstance().showFloatWindow(true);
//            LogUtils.d(TAG, "gesture cancelled");
        }
    };


    /**
     * 手势活动
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void doGestureToScreen(OperationModel operationModel){
        FloatWindowManager.getInstance().showFloatWindow(false);
//        if(operationModel.getDurationTime() <= 100){ //单击
//            LogUtils.e(TAG, "单击");
//            Float[] point = operationModel.getPointList().get(operationModel.getPointList().size() - 1);
//           AccessibilityUtil.INSTANCE.clickOnScreen(DYAccessibilityService.this, point[0], point[1],callback,null);
//        }else{ //手势
//            if(operationModel.getPointList().size() > 1){
//                LogUtils.e(TAG, "手势");
//                Path path = new Path();
//                if(operationModel.getDownPoint() != null){
//                    path.moveTo(operationModel.getDownPoint()[0], operationModel.getDownPoint()[1]);
//                }
//                List<Float[]> pointModels = operationModel.getPointList();
//                for(int i = 0; i < pointModels.size(); i++){
//                    path.lineTo(pointModels.get(i)[0], pointModels.get(i)[1]);
//                }
//                AccessibilityUtil.INSTANCE.gestureOnScreen(DYAccessibilityService.this,path,0,operationModel.getDurationTime(),callback,null);
//            }else {
//                FloatWindowManager.getInstance().showFloatWindow(true);
//            }
//        }

        Path path = new Path();
        if(operationModel.getDownPoint() != null){
            path.moveTo(operationModel.getDownPoint()[0], operationModel.getDownPoint()[1]);
        }
        List<Float[]> pointModels = operationModel.getPointList();
        for(int i = 0; i < pointModels.size(); i++){
            path.lineTo(pointModels.get(i)[0], pointModels.get(i)[1]);
        }
        AccessibilityUtil.INSTANCE.gestureOnScreen(DYAccessibilityService.this,path,0,operationModel.getDurationTime(),callback,null);
    }

    synchronized public void startTimer(){
        mHandler.removeCallbacks(heartBeatRunnable);
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
    }

    synchronized public void cancelTimer(){
        if(mHandler != null && heartBeatRunnable != null){
            mHandler.removeCallbacks(heartBeatRunnable);
        }
    }

    synchronized public void startTimer1(){
        mHandler1.removeCallbacks(heartBeatRunnable1);
        mHandler1.postDelayed(heartBeatRunnable1, HEART_BEAT_RATE);//开启心跳检测
    }

    synchronized public void cancelTimer1(){
        if(mHandler1 != null && heartBeatRunnable1 != null){
            mHandler1.removeCallbacks(heartBeatRunnable1);
        }
    }

    //    -------------------------------------Handler定时任务------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            //每隔一定的时间，检查socket连接
            mHandler.postDelayed(this, HEART_BEAT_RATE);

            heartBeat1 ++;
            heartBeat2 = 0;
            if(heartBeat1 > 2){//daemonThreadDisposable 已经很久没给heartBeat1清零了，估计死了
                heartBeat1 = 0;
                startTimer1();
            }

            //检查nettyServer有没有断开
        }
    };

    //------------------------------rxjava定时任务------------------------------------------------------------------------
    private Handler mHandler1 = new Handler();
    private Runnable heartBeatRunnable1 = new Runnable() {
        @Override
        public void run() {
            sendScreenImg();
            //每隔一定的时间，检查socket连接
            mHandler1.postDelayed(this, 1000);

            heartBeat1 = 0;
            heartBeat2 ++;
            if(heartBeat2 > 12){//heartBeatRunnable 已经很久没给heartBeat2清零了，估计死了
                heartBeat2 = 0;
                startTimer();
            }
        }
    };

}
