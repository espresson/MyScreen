package com.example.screenserver.websocket;



import com.example.screenserver.utils.LogUtils;

import org.java_websocket.WebSocket;

import java.util.Arrays;

public class SocketClientBean {
    private final String TAG = "SocketServer";

    private WebSocket conn;
    private String host;
    private String type; //Accessibility-远程协助
    private int heartBeat;

    public SocketClientBean() {
    }

    public SocketClientBean(WebSocket conn, String host) {
        this.conn = conn;
        this.host = host;
    }

    public void sendData(String msg){
        if(conn != null && conn.isOpen()){
            LogUtils.d(TAG, String.format("向%s发送消息->%s",host,msg));
            conn.send(msg);
        }
    }

    public void sendData(byte[] bytes){
        if(conn != null && conn.isOpen()){
            LogUtils.d(TAG, String.format("向%s发送消息->%s",host, Arrays.toString(bytes)));
            conn.send(bytes);
        }
    }

    public void close(){
        if(conn != null && conn.isOpen()){
            conn.close();
        }
    }

    public WebSocket getConn() {
        return conn;
    }

    public void setConn(WebSocket conn) {
        this.conn = conn;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getHeartBeat() {
        return heartBeat;
    }

    public void setHeartBeat(int heartBeat) {
        this.heartBeat = heartBeat;
    }
}
