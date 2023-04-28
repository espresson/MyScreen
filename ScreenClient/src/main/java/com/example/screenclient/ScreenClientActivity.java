package com.example.screenclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.screenclient.Utils.ImageUtil;
import com.example.screenclient.Utils.LogUtils;
import com.example.screenclient.Utils.ScreenUtil;
import com.example.screenclient.bean.OperationModel;
import com.example.screenclient.bean.RemoteAssistanceBean;
import com.example.screenclient.decode.Decode264;
import com.example.screenclient.view.MySurfaceView;
import com.example.screenclient.websocket.OnSocketMessage;
import com.example.screenclient.websocket.SocketServer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenClientActivity extends Activity {
    private final String TAG = ScreenClientActivity.class.getSimpleName();

    private RemoteAssistanceBean remoteAssistanceBean = new RemoteAssistanceBean();
    private SurfaceView mSurfaceView;

    private ImageView ivScreen;

    private TextView textView;

    private Surface mSurface;

    private Decode264 mDecode264;

    private SocketServer socketServer;

    private int screenWidth,screenHeight;

    private OperationModel operationModel;

    private long dateStart, dateEnd, during;

    private Float progress = 0f;
    private Handler handler =new  Handler(Looper.getMainLooper());

    private Timer timer;
    private TimerTask timerTask;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_client);
        mSurfaceView =findViewById(R.id.surfaceView);
        ivScreen = findViewById(R.id.image);
        textView= findViewById(R.id.textview);
        ivScreen.setOnTouchListener(onTouchListener);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mSurface = surfaceHolder.getSurface();
                mDecode264 = new Decode264(mSurface);
               initSocket();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });

//        timer = new Timer();
//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                long lastRenderTime = mSurfaceView.getLastRenderTime();
//                runOnUiThread(() -> textView.setText("" + lastRenderTime + "ms"));
//            }
//        };
//        timer.schedule(timerTask, 0, 1000);


        new Handler().postDelayed(this::resetSurfaceViewSize,500);


        switchScreen();
    }


    private void drawFrame(Canvas canvas) {
        // 绘制逻辑
    }

    private void resetSurfaceViewSize(){
        if(screenWidth == 0) return;
        int width = ScreenUtil.getScreenWidth(this);
        int height = ScreenUtil.getScreenHeight(this);
        int sw = width;
        int sh = width * screenHeight / screenWidth;
        if(sh > height){
            sh = height;
            sw = height * screenWidth / screenHeight;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        layoutParams.width = sw;
        layoutParams.height = sh;
        mSurfaceView.setLayoutParams(layoutParams);
    }


    private void initSocket() {
        String host =getIntent().getStringExtra("ipAddress");
        socketServer = new SocketServer(String.format("ws://%s:11006/?type=Accessibility",host));
        socketServer.setOnSocketMessage(onSocketMessage);
        socketServer.initAndStart();

    }

    private OnSocketMessage onSocketMessage = new OnSocketMessage() {
        @Override
        public void onMessage(String message) {
            runOnUiThread(() -> {
                try {
                    RemoteAssistanceBean bean = JSON.parseObject(message,RemoteAssistanceBean.class);
                    switch (bean.getCode()){
                        case 1:
                            Bitmap bitmap = ImageUtil.base64ToBitmap(bean.getData());
                            ivScreen.setImageBitmap(bitmap);
                            if(screenWidth == 0){
                                screenWidth = bitmap.getWidth();
                                screenHeight = bitmap.getHeight();
                            }
                            break;
                        case 8:
                            String[] ss = bean.getData().split(",");
                            screenWidth = Integer.parseInt(ss[0]);
                            screenHeight = Integer.parseInt(ss[1]);
                            break;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onMessage(byte[] bytes) {
            if(mDecode264 != null){
                mDecode264.setData(bytes);
            }
        }
    };


    boolean isEffective = true; //此次手势是否有效的
    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Date date = new Date();
            if (view.getId() == R.id.image) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_DOWN = " + motionEvent.getAction());
                        if (operationModel == null) {
                            operationModel = new OperationModel();
                        }
                        operationModel.clear();
                        Float[] point = getScreenPoint(motionEvent.getX(), motionEvent.getY());
                        if(point != null){
                            isEffective = true;
                            dateStart = date.getTime();
                            operationModel.setDownPoint(point);
                        }else {
                            isEffective = false;
                            operationModel.setDownPoint(null);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_MOVE = " + motionEvent.getAction());
                        if(!isEffective) break;
                        point = getScreenPoint(motionEvent.getX(), motionEvent.getY());
                        if(point != null){
                            operationModel.addLocationModel(point);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        LogUtils.d(TAG, "onTouchEvent: ACTION_UP = " + motionEvent.getAction());
                        if(!isEffective) break;
                        point = getScreenPoint(motionEvent.getX(), motionEvent.getY());
                        if(point != null){
                            dateEnd = date.getTime();
                            during = dateEnd - dateStart;
                            operationModel.addLocationModel(point);
                            operationModel.setDelayTime(0);
                            operationModel.setDurationTime((int) during);
                            remoteAssistanceBean.setCode(6);
                            remoteAssistanceBean.setData(JSON.toJSONString(operationModel));
                            sendMessage("RemoteAssistance"+JSON.toJSONString(remoteAssistanceBean));
                            LogUtils.d(TAG, "onTouch: during:" + during);
//                            if(during <= 100){
//                                RemoteAssistanceBean bean = new RemoteAssistanceBean(5, JSON.toJSONString(operationModel));
//                                sendMessage(JSON.toJSONString(bean));
//                            }else{
//                                if(operationModel.getPointList().size() > 3){
//                                    RemoteAssistanceBean bean = new RemoteAssistanceBean(6, JSON.toJSONString(operationModel));
//                                    sendMessage(JSON.toJSONString(bean));
//                                }
//                            }
                        }
                        break;
                }
            }
            return false;
        }
    };


    private void sendAction(int during){
        operationModel.setDurationTime(during);
        remoteAssistanceBean.setCode(6);
        remoteAssistanceBean.setData(JSON.toJSONString(operationModel));
        String str = JSON.toJSONString(remoteAssistanceBean);
        operationModel.setDownPoint(operationModel.getPointList().get(operationModel.getPointList().size() - 1));
        operationModel.clear();
        sendMessage("RemoteAssistance"+str);
    }

    private void sendMessage(String msg){
        if (!socketServer.isConnecting()) {
            Toast.makeText(getApplicationContext(), "未连接,请先连接", Toast.LENGTH_SHORT).show();
        } else {
            if (TextUtils.isEmpty(msg.trim())) {
                return;
            }
            socketServer.sendMsg(msg);
        }
    }

    /**
     * 获取远程屏幕的实际坐标，
     * 显示方式 ： scaleType="fitCenter"
     */
    private Float[] getScreenPoint(float x, float y){
        LogUtils.d(TAG, "onTouchEvent: getX:" + x);
        LogUtils.d(TAG, "onTouchEvent: getY:" + y);
        if(screenWidth == 0) return null;
        float screenX;
        float screenY;
        int imgWidth = ivScreen.getWidth();
        int imgHeight = ivScreen.getHeight();
        LogUtils.d(TAG, "\n\n");
        LogUtils.d(TAG, String.format("test----------------------: 远程屏幕，w = %s，h = %s",screenWidth,screenHeight));
        LogUtils.d(TAG, String.format("test----------------------: imageView，w = %s，h = %s",imgWidth,imgHeight));
        if(1.0f * imgWidth/imgHeight > 1.0f * screenWidth/screenHeight){
            //imgWidth两边会多出空隙
            LogUtils.d(TAG, "test----------------------: 两边多出空隙");
            int sw = imgHeight * screenWidth / screenHeight; //客户端显示远程屏幕的宽度
            LogUtils.d(TAG, String.format("test----------------------: 远程屏幕在图片上宽高 ，w = %s, h = %s" , sw, imgHeight));
            float sx = x - ((imgWidth - sw) >> 1); //客户端显示远程屏幕的宽度上的x坐标
            if(sx < 0 || sx > sw) return null; //点击了远程屏幕的外面
            float scale = 1.0f * screenWidth / sw;
            screenX = sx * scale;
            screenY = y * scale;
            LogUtils.d(TAG, "test----------------------: 远程屏幕宽高/实际显示图片上的宽高 scale = " + scale);
            LogUtils.d(TAG, String.format("test----------------------: 点击事件在图片上的坐标，x = %s，y = %s",sx,y));
        }else {
            LogUtils.d(TAG, "test----------------------: 上下多出空隙");
            int sh = imgWidth * screenHeight / screenWidth;
            LogUtils.d(TAG, String.format("test----------------------: 远程屏幕在图片上宽高 ，w = %s, h = %s" , imgWidth, sh));
            float sy = y - ((imgHeight - sh) >> 1);
            if(sy < 0 || sy > sh) return null; //点击了远程屏幕的外面
            float scale = 1.0f * screenWidth / imgWidth;
            screenX = x * scale;
            screenY = sy * scale;
            LogUtils.d(TAG, "test----------------------: 远程屏幕宽高/实际显示图片上宽高 scale = " + scale);
            LogUtils.d(TAG, String.format("test----------------------: 点击事件在图片上的坐标，x = %s，y = %s",x,sy));
        }
        LogUtils.d(TAG, String.format("test----------------------: 转换成实际屏幕上的坐标，x = %s，y = %s",screenX,screenY));
        return new Float[]{Math.round(screenX * 100) / 100.0f, Math.round(screenY * 100) / 100.0f};
    }

    private void showConfirmDialog(String msg){
        if(isFinishing()) return;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton("确定",null)
                .show();
    }


    /**
     * 横竖屏切换
     */
    private void switchScreen(){
        if(ScreenUtil.isLandscape(this)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    //android实现左右滑动两次返回

 private long firstTime = 0;
    @Override
    public void onBackPressed() {
        if(firstTime+2000> System.currentTimeMillis()) {
            super.onBackPressed();
        }else{
            Toast.makeText(this,"再按一次退出",Toast.LENGTH_SHORT).show();
        }
        firstTime = System.currentTimeMillis();

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetSurfaceViewSize();
        if (newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE){
            //如果是横屏了，在这里设置横屏的UI
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            //否则，在这里设置竖屏的UI
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        socketServer.closeConnect();
        super.onDestroy();
        timer.cancel();
        timerTask.cancel();
    }

}