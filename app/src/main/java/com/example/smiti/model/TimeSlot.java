package com.example.smiti.model;

import java.io.Serializable;

/**
 * 스터디 가능 시간 슬롯을 나타내는 클래스
 */
public class TimeSlot implements Serializable {
    private String startTime; // 시작 시간 (HH:MM 형식)
    private String endTime;   // 종료 시간 (HH:MM 형식)

    public TimeSlot(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * 시간 범위 문자열 생성 (예: "10:00 ~ 12:00")
     */
    public String getTimeRangeString() {
        return startTime + " ~ " + endTime;
    }

    @Override
    public String toString() {
        return startTime + "~" + endTime;
    }
} 