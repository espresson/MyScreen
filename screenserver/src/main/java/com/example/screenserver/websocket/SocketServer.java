package com.example.screenserver.websocket;

import android.os.Handler;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.screenserver.Application;
import com.example.screenserver.bean.RemoteAssistanceBean;
import com.example.screenserver.event.NettyInitEvent;
import com.example.screenserver.utils.LogUtils;
import com.example.screenserver.utils.ScreenUtil;


import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;

public class SocketServer extends WebSocketServer {
    private final String TAG = "SocketServer";
    private final String HEART_BEAT = "HeartBeat";
    private HashMap<String, SocketClientBean> webSocketClients = new HashMap<>();

    private OnSocketMessage onSocketMessage;
    private String socketStatus = "";

    public SocketServer(InetSocketAddress inetSocketAddress){
        super(inetSocketAddress);
    }

    public void setOnSocketMessage(OnSocketMessage onSocketMessage) {
        this.onSocketMessage = onSocketMessage;
    }

    public String getSocketStatus() {
        return socketStatus;
    }

    public void setSocketStatus(String socketStatus) {
        this.socketStatus = socketStatus;
        LogUtils.d(TAG,socketStatus);
        EventBus.getDefault().post(new NettyInitEvent(socketStatus));
    }

    private Handler mHandler = new Handler(msg -> {
        switch (msg.what){
            case 1:
                Toast.makeText(Application.getmApp(),(String) msg.obj,Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    });

    @Override
    public void start() {
        super.start();
        setSocketStatus("start() 服务器已就绪");
        heartBeatHandler.removeCallbacks(heartBeatRunnable);
        heartBeatHandler.postDelayed(heartBeatRunnable,HEART_BEAT_RATE);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        setSocketStatus(String.format("onOpen 连接成功:%s%s", getRemoteSocketAddress(conn), conn.getResourceDescriptor()));

        String host = getRemoteSocketAddress(conn).getHostName();
        SocketClientBean clientBean = new SocketClientBean(conn,host);
        String[] params = handshake.getResourceDescriptor().substring(handshake.getResourceDescriptor().indexOf("?")+1).split("&");
        for(String str: params){
            String[] param = str.split("=");
            if(param.length < 2){
                continue;
            }

            if("type".equals(param[0])){//远程协助的类型
                if("Accessibility".equals(param[1])){
                    close("Accessibility"); //把其他远程协助挤下线
                    String data = String.format("%s,%s", ScreenUtil.getScreenWidth(Application.getmApp()), ScreenUtil.getScreenHeight(Application.getmApp()));
                    clientBean.sendData(JSON.toJSONString(new RemoteAssistanceBean(8, data)));
                    mHandler.sendMessage(mHandler.obtainMessage(1,"connect success！！！")); //首次连接成功弹出提示使窗口发生变化更新屏幕，否则首次连接会黑屏
                }
                clientBean.setType(param[1]);
                webSocketClients.put(host,clientBean);
            }

        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(getRemoteSocketAddress(conn)!=null){
            String msg = String.format("onClose() %s连接断开, remote:%s, code:%s, reason:%s",getRemoteSocketAddress(conn).getHostName(),remote,code,reason);
            setSocketStatus(msg);
            webSocketClients.remove(getRemoteSocketAddress(conn).getHostName());
        }

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if(message == null) return;
        if(HEART_BEAT.equals(message)){
            SocketClientBean bean = webSocketClients.get(getRemoteSocketAddress(conn).getHostName());
            if(bean != null){
                bean.setHeartBeat(0);
                sendToClient(bean, HEART_BEAT);
            }
        }else {
            LogUtils.d(TAG, String.format("收到%s的消息：%s",getRemoteSocketAddress(conn).getHostName(),message));
            if(onSocketMessage != null){
                onSocketMessage.onMessage(conn, message);
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer bytes) {
        byte[] buf = new byte[bytes.remaining()];
        bytes.get(buf);
        LogUtils.d(TAG, String.format("收到%s的消息：%s",getRemoteSocketAddress(conn).getHostName(),new String(buf, Charset.forName("UTF-8"))));
        if(onSocketMessage != null){
            onSocketMessage.onMessage(conn, buf);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        setSocketStatus(String.format("onError：%s" , ex.getMessage()));
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        LogUtils.d(TAG,"onStart()");
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        super.stop();
        LogUtils.d(TAG,"stop()");
    }

    public boolean isClientConnect(){
        return webSocketClients.size() > 0;
    }

    public void sendToClient(SocketClientBean clientBean, String msg){
        if(clientBean != null){
            clientBean.sendData(msg);
        }
    }

    public void sendToClient(SocketClientBean clientBean, byte[] bytes){
        if(clientBean != null){
            clientBean.sendData(bytes);
        }
    }

    /**
     * @param type Accessibility-远程协助
     */
    public void sendData(String type, String msg){
        Set<String> keys = webSocketClients.keySet();
        for(String key: keys){
            SocketClientBean bean = webSocketClients.get(key);
            if(bean != null && bean.getType().equals(type)){
                sendToClient(bean, msg);
            }
        }
    }

    /**
     * @param type Accessibility-远程协助
     */
    public void sendData(String type, byte[] bytes){
        Set<String> keys = webSocketClients.keySet();
        for(String key: keys){
            SocketClientBean bean = webSocketClients.get(key);
            if(bean != null && bean.getType().equals(type)){
                sendToClient(bean, bytes);
            }
        }
    }

    public void close(){
        LogUtils.d(TAG,"close() 关闭所有连接");
        Set<String> keys = webSocketClients.keySet();
        for(String key: keys){
            SocketClientBean bean = webSocketClients.get(key);
            if(bean != null){
                bean.close();
                webSocketClients.remove(key);
            }
        }
    }

    public void close(String type){
        LogUtils.d(TAG,"close() 关闭远程连接类型 " + type);
        Set<String> keys = webSocketClients.keySet();
        for(String key: keys){
            SocketClientBean bean = webSocketClients.get(key);
            if(bean != null && type.equals(bean.getType())){
                bean.sendData("exit0");
                bean.close();
                webSocketClients.remove(key);
            }
        }
    }

    //    -------------------------------------WebSocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler heartBeatHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            Set<String> keys = webSocketClients.keySet();
            for(String key: keys){
                SocketClientBean bean = webSocketClients.get(key);
                if(bean != null){
                    if(bean.getHeartBeat() > 1){
                        //已经至少2次没有心跳消息
                        bean.close();
                        webSocketClients.remove(key);
                    }else {
                        bean.setHeartBeat(bean.getHeartBeat() + 1);
                    }
                }
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            heartBeatHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };
}
