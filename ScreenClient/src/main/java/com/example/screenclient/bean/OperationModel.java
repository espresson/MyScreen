package com.example.screenclient.bean;

import java.util.ArrayList;
import java.util.List;

public class OperationModel {
    private int  durationTime;
    private int  delayTime = 0;
    private int action; // 0-ACTION_DOWN, 1-ACTION_UP, 2-ACTION_MOVE
    private Float[] downPoint;
    private List<Float[]> pointList = new ArrayList<>();

    public void clear(){
        if(pointList != null){
            pointList.clear();
        }
    }

    public void addLocationModel(Float[] point){
        pointList.add(point);
    }

    public int getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(int durationTime) {
        this.durationTime = durationTime;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Float[] getDownPoint() {
        return downPoint;
    }

    public void setDownPoint(Float[] downPoint) {
        this.downPoint = downPoint;
    }

    public List<Float[]> getPointList() {
        return pointList;
    }

    public void setPointList(List<Float[]> pointList) {
        this.pointList = pointList;
    }
}
