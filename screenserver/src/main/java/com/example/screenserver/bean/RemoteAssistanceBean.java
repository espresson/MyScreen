package com.example.screenserver.bean;


public class RemoteAssistanceBean {
    private int code = 1; // 1-图片的base64，2-BACK，3-HOME，4-长按，5-单击事件，6-手势功能，7-重启，8-远程屏幕的宽高
    private String msg;
    private String data;

    public RemoteAssistanceBean() {
    }

    public RemoteAssistanceBean(int code, String data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
