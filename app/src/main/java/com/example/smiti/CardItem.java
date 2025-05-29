package com.example.smiti;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CardItem {
    private int imageResource;
    private String title;
    private String subtitle;
    private Class<?> activityToOpen; // 클릭 시 열릴 액티비티
    private String category;         // 스터디 카테고리
    private Calendar studyDate;      // 스터디 시작일

    public CardItem(int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate) {
        this.imageResource = imageResource;
        this.title = title;
        this.subtitle = subtitle;
        this.activityToOpen = activityToOpen;
        this.category = category;
        this.studyDate = studyDate;
    }

    public int getImageResource() {
        return imageResource;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
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

    public String getStudyDateFormatted() {
        if (studyDate == null) {
            return "날짜 미정";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        return sdf.format(studyDate.getTime());
    }
}
