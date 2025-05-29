package com.example.smiti;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CardDataHolder {

    // 앱이 실행되는 동안 유지될 '인기 그룹' 데이터 리스트
    public static List<CardItem> popularItems = new ArrayList<>();
    private static boolean isPopularItemsInitialDataAdded = false;

    // SMBTI 그룹 데이터 리스트 (필요에 따라 이 방식도 사용할 수 있습니다)
    // public static List<CardItem> smbtiItems = new ArrayList<>();
    // private static boolean isSmbtiItemsInitialDataAdded = false;

    // 인기 그룹 초기 데이터 (앱 시작 시 한 번만 실행되도록)
    public static void initializePopularItemsIfNeeded() {
        if (!isPopularItemsInitialDataAdded && popularItems.isEmpty()) {
            // 예시: HomeDashboardActivity의 onCreate에서 popularItems에 추가하던 초기 데이터들
            popularItems.add(new CardItem(R.drawable.image1, "코딩의 신", "자바 기초 스터디 모집", GroupSearchActivity.class, "프로그래밍", createCalendar(2025, Calendar.JUNE, 10)));
            popularItems.add(new CardItem(R.drawable.image2, "영어 정복", "매일 영어 회화", GroupSearchActivity.class, "어학", createCalendar(2025, Calendar.JULY, 1)));
            popularItems.add(new CardItem(R.drawable.image3, "알고리즘 격파", "PS 스터디", GroupSearchActivity.class, "프로그래밍", createCalendar(2025, Calendar.MAY, 20)));
            // ... 기타 초기 아이템들

            isPopularItemsInitialDataAdded = true; // 초기 데이터 추가 완료
        }
    }

    // 아이템 추가 (항상 맨 앞에 추가)
    public static void addPopularItem(CardItem item) {
        popularItems.add(0, item);
    }

    // 아이템 삭제
    public static void removePopularItem(int position) {
        if (position >= 0 && position < popularItems.size()) {
            popularItems.remove(position);
        }
    }

    // 아이템 리스트 가져오기
    public static List<CardItem> getPopularItems() {
        return popularItems;
    }

    // Calendar 객체 생성 헬퍼 메소드
    public static Calendar createCalendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        // Calendar의 month는 0부터 시작 (예: Calendar.JANUARY는 0)
        calendar.set(year, month - 1, day);
        return calendar;
    }

    // --- SMBTI 아이템 관련 메소드 (필요하다면 아래와 같이 추가) ---
    /*
    public static void initializeSmbtiItemsIfNeeded() {
        if (!isSmbtiItemsInitialDataAdded && smbtiItems.isEmpty()) {
            smbtiItems.add(new CardItem(R.drawable.sample_image_4, "INFP 모여라", "감성 토론방", null, "INFP", createCalendar(2025, Calendar.AUGUST, 5)));
            smbtiItems.add(new CardItem(R.drawable.sample_image_5, "ESTJ 스터디", "계획적인 스터디", null, "ESTJ", createCalendar(2025, Calendar.SEPTEMBER, 15)));
            isSmbtiItemsInitialDataAdded = true;
        }
    }

    public static void addSmbtiItem(CardItem item) {
        smbtiItems.add(0, item);
    }

    public static void removeSmbtiItem(int position) {
        if (position >= 0 && position < smbtiItems.size()) {
            smbtiItems.remove(position);
        }
    }

    public static List<CardItem> getSmbtiItems() {
        return smbtiItems;
    }
    */
}