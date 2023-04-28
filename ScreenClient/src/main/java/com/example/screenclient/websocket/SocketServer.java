package com.example.screenclient.websocket;

import android.os.Handler;
import android.util.Log;


import com.example.screenclient.Utils.LogUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SocketServer {

    private final String TAG = "SocketServer";
    private WebSocketClient socketClient;

    private static final long CLOSE_RECON_TIME = 5000;//连接断开或者连接错误立即重连
    private final String HEART_BEAT = "HeartBeat";
    private boolean isCancelConnect = false; //是否主动断开，不想连接了
    private int heartBeatDetect = -1;

    private String wsUrl;

    private OnSocketMessage onSocketMessage;

    public SocketServer(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public void setOnSocketMessage(OnSocketMessage onSocketMessage) {
        this.onSocketMessage = onSocketMessage;
    }

    public void initAndStart(){
        start();
        mHandler.removeCallbacks(heartBeatRunnable);
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
    }

    private void start(){
        try {
            URI uri = new URI(wsUrl);
            socketClient = new SocketClient(uri);
            connect();
//            socketClient.connect();
        } catch (Exception e) {
            Log.e(TAG,"error:"+e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 连接WebSocket
     */
    private void connect() {
        if (socketClient != null && !socketClient.isOpen()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isCancelConnect = false;
                    if (socketClient.getReadyState().equals(ReadyState.NOT_YET_CONNECTED)){
                        try {
                            //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                            socketClient.connectBlocking();
                        } catch (Exception e) {
                        }
                    }else if (socketClient.getReadyState().equals(ReadyState.CLOSING) || socketClient.getReadyState().equals(ReadyState.CLOSED)){
                        try {
                            socketClient.reconnectBlocking();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
        }
    }

    public void sendMsg(String msg) {
        sendMsg(msg,false);
    }

    /**
     * 发送消息
     */
    public void sendMsg(String msg,boolean isHeartBeat) {
        if (null != socketClient) {
            LogUtils.i(TAG, "--> " + msg);
            if(!isHeartBeat){ // 心跳消息不保存
//                LogManage.addLog(TAG,"--> " + msg);
            }
            try {
                socketClient.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 断开连接
     */
    public void closeConnect() {
        isCancelConnect = true;
        if(mHandler != null && heartBeatRunnable != null){
            mHandler.removeCallbacks(heartBeatRunnable);
        }
        try {
            if (null != socketClient) {
                socketClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socketClient = null;
        }
    }

    /**
     * socket是否连接成功
     * @return
     */
    public boolean isConnecting(){
        if(socketClient != null && socketClient.isOpen()){
            return true;
        }else {
            start();
//            EventBus.getDefault().post("reconnectWebSocket");
            return false;
        }
    }

    //    -------------------------------------WebSocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if(heartBeatDetect >= 0){
                heartBeatDetect ++;
            }
            if(!isCancelConnect && heartBeatDetect > 2){
                //2次没有心跳回应，发起重连
//                LogManage.addLog(TAG,"2次没有心跳回应，发起重连");
                if(socketClient == null){
                    start();
                }else {
                    connect();
                }
            }else if (socketClient != null) {
                if (socketClient.isClosed() && !isCancelConnect) {
                    LogUtils.e(TAG, "心跳包检测WebSocket连接状态：已关闭，开启重连");
                    connect();
                } else if (socketClient.isOpen()) {
                    LogUtils.d(TAG, "心跳包检测WebSocket连接状态：已连接");
                    sendMsg(HEART_BEAT,true);
                } else {
                    LogUtils.e(TAG, "心跳包检测WebSocket连接状态：已断开");
                }
            } else if(!isCancelConnect){
                //如果client已为空，重新初始化连接
                if(socketClient == null){
                    start();
                    LogUtils.e(TAG, "心跳包检测WebSocket连接状态：client已为空，重新初始化连接");
                }
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    private class SocketClient extends WebSocketClient {

        public SocketClient(URI serverUri) {
            super(serverUri, new Draft_6455());
            Log.d(TAG,"new SocketClient");
        }

        @Override
        public void onOpen(ServerHandshake handShakeData) {
            String msg = String.format("onOpen()连接成功, HttpStatus:%s, HttpStatusMessage:%s",handShakeData.getHttpStatus(),handShakeData.getHttpStatusMessage());
            LogUtils.i(TAG, msg);
//            sendMsg("初始化");
        }

        @Override
        public void onMessage(String message) {
            if(HEART_BEAT.equals(message)){
                heartBeatDetect = 0;
            }else {
                Log.d(TAG,"<-- " + message);
                if("exit0".equals(message)){
                    closeConnect(); //被挤下线
                }else if(onSocketMessage != null){
                    onSocketMessage.onMessage(message);
                }
            }
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            Log.d(TAG,"<-- " + Arrays.toString(buf));
            if(onSocketMessage != null){
                onSocketMessage.onMessage(buf);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            String msg = String.format("onClose() 连接断开, remote:%s, code:%s, reason:%s",remote,code,reason);
            Log.d(TAG, msg);
            mHandler.removeCallbacks(heartBeatRunnable);
            mHandler.postDelayed(heartBeatRunnable, CLOSE_RECON_TIME);//开启心跳检测
        }

        @Override
        public void onError(Exception ex) {
            Log.d(TAG,"onError ="+ex.toString());
            mHandler.removeCallbacks(heartBeatRunnable);
            mHandler.postDelayed(heartBeatRunnable, CLOSE_RECON_TIME);//开启心跳检测
        }
    }

}
