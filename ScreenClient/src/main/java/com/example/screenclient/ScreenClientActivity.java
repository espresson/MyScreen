package com.example.screenclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.example.screenclient.Utils.ImageUtil;
import com.example.screenclient.Utils.ScreenUtil;
import com.example.screenclient.bean.RemoteAssistanceBean;
import com.example.screenclient.decode.Decode264;
import com.example.screenclient.websocket.OnSocketMessage;
import com.example.screenclient.websocket.SocketServer;

public class ScreenClientActivity extends AppCompatActivity {
    private final String TAG = ScreenClientActivity.class.getSimpleName();

    private RemoteAssistanceBean remoteAssistanceBean = new RemoteAssistanceBean();
    private SurfaceView mSurfaceView;

    private ImageView ivScreen;

    private Surface mSurface;

    private Decode264 mDecode264;

    private SocketServer socketServer;

    private int screenWidth,screenHeight;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_client);
        mSurfaceView = findViewById(R.id.surfaceView);
        ivScreen = findViewById(R.id.image);
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
        new Handler().postDelayed(this::resetSurfaceViewSize,500);

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
    }

}