package com.example.smiti;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CardItem {
    private int imageResource;
    private String title;
    private String subtitle; // 그룹 설명으로 사용될 필드
    private Class<?> activityToOpen; // 클릭 시 열릴 액티비티
    private String category;         // 스터디 카테고리
    private Calendar studyDate;      // 스터디 시작일
    private int maxMembers;          // 스터디 최대 인원 (새로 추가된 필드)
    private int currentMembers;       // 현재 인원 필드 추가
    private String groupId;           // 그룹 ID 필드 추가


    public CardItem(int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate, int maxMembers, int currentMembers, String groupId) {
        this.imageResource = imageResource;
        this.title = title;
        this.subtitle = subtitle;
        this.activityToOpen = activityToOpen;
        this.category = category;
        this.studyDate = studyDate;
        this.maxMembers = maxMembers;
        this.currentMembers = currentMembers; // 현재 인원 설정
        this.groupId = groupId; // 그룹 ID 설정
    }

    public int getImageResource() {
        return imageResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() { // subtitle getter 이름 변경
        return subtitle;
    }

    public Class<?> getActivityToOpen() {
        return activityToOpen;
    }

    public String getCategory() {
        return category;
    }

    public Calendar getStudyDate() {
        return studyDate;
    }


    public int getMaxMembers() {
        return maxMembers;
    }

    // currentMembers getter 추가
    public int getCurrentMembers() {
        return currentMembers;
    }

    public String getGroupId() { // 그룹 ID getter 추가
        return groupId;
    }

    public String getStudyDateFormatted() {
        if (studyDate == null) {
            return "날짜 미정";
        }
        // 기존 포맷 유지 또는 "yyyy-MM-dd" 등으로 변경 가능
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        return sdf.format(studyDate.getTime());
    }
}
