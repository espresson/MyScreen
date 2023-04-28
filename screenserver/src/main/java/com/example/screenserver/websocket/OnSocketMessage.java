package com.example.screenserver.websocket;

import org.java_websocket.WebSocket;

public abstract class OnSocketMessage {

    public void onMessage(WebSocket conn, String message){};

    public void onMessage(WebSocket conn, byte[] bytes){};
}
