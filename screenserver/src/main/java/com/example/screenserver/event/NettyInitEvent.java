package com.example.screenserver.event;


public class NettyInitEvent {

    private String msg;

    public NettyInitEvent(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
