package com.example.screenserver.websocket;

import android.media.projection.MediaProjection;


import com.example.screenserver.live.CodecH264;
import com.example.screenserver.live.CodecH265;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SocketService {

    private static final String TAG = "SocketService";
    private int port = 11006;
    private SocketServer webSocketServer;

    private CodecH264 mCodecH264;

    private static volatile SocketService instance;

    public static  SocketService getInstance(){
        if(instance == null){
            synchronized (SocketService.class){
                if(instance == null){
                    instance = new SocketService();
                }
            }
        }
        return instance;
    }

    public SocketService(){
        webSocketServer = new SocketServer(new InetSocketAddress(port));

        //webSocketServer.close(); //把其他远程协助挤下线
    }

    public void startLive(MediaProjection mediaProjection){
        mCodecH264 = new CodecH264(this, mediaProjection);
        mCodecH264.startLive();
    }

    public void start(){
        webSocketServer.setReuseAddr(true);
        webSocketServer.start();
    }

    public void close(){
        try {
            webSocketServer.stop();
            webSocketServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isClientConnect(){
        return webSocketServer.isClientConnect();
    }

    public void sendData(String msg){
        webSocketServer.sendData("Accessibility",msg);
    }

    public void sendData(byte[] bytes){
        webSocketServer.sendData("Accessibility",bytes);
    }

    public void setOnSocketMessage(OnSocketMessage onSocketMessage) {
        webSocketServer.setOnSocketMessage(onSocketMessage);
    }

    public String getSocketStatus() {
        return webSocketServer.getSocketStatus();
    }

}
