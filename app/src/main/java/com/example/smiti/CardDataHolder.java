package com.example.smiti;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CardDataHolder {

    // 앱이 실행되는 동안 유지될 '인기 그룹' 데이터 리스트
    public static List<CardItem> popularItems = new ArrayList<>();
    private static boolean isPopularItemsInitialDataAdded = false;

    // 인기 그룹 초기 데이터 (앱 시작 시 한 번만 실행되도록)
    public static void initializePopularItemsIfNeeded() {
        if (!isPopularItemsInitialDataAdded && popularItems.isEmpty()) {
            // CardItem 생성자:
            // (int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate, int maxMembers)
            // 마지막 인자로 maxMembers 값을 추가합니다. 예시로 5 또는 10을 사용합니다.
            popularItems.add(new CardItem(R.drawable.image1, "코딩의 신", "자바 기초 스터디 모집", GroupSearchActivity.class, "프로그래밍", createCalendar(2025, Calendar.JUNE, 10), 10)); // 예: 최대 10명
            popularItems.add(new CardItem(R.drawable.image2, "영어 정복", "매일 영어 회화", GroupSearchActivity.class, "어학", createCalendar(2025, Calendar.JULY, 1), 5));     // 예: 최대 5명
            popularItems.add(new CardItem(R.drawable.image3, "알고리즘 격파", "PS 스터디", GroupSearchActivity.class, "프로그래밍", createCalendar(2025, Calendar.MAY, 20), 8));      // 예: 최대 8명
            // ... 기타 초기 아이템들도 maxMembers 값을 포함하여 추가 ...

            isPopularItemsInitialDataAdded = true; // 초기 데이터 추가 완료
        }
    }

    // 아이템 추가 (항상 맨 앞에 추가)
    public static void addPopularItem(CardItem item) {
        if (item != null) {
            popularItems.add(0, item);
        }
    }

    // 아이템 삭제
    public static void removePopularItem(int position) {
        if (position >= 0 && position < popularItems.size()) {
            popularItems.remove(position);
        }
    }

    // 아이템 리스트 가져오기
    public static List<CardItem> getPopularItems() {
        // 필요하다면 여기서도 initializePopularItemsIfNeeded()를 호출하여
        // popularItems가 초기화되지 않은 상태로 반환되는 것을 방지할 수 있습니다.
        // initializePopularItemsIfNeeded(); // 주석 해제하여 항상 초기화 보장
        return popularItems;
    }

    // Calendar 객체 생성 헬퍼 메소드
    public static Calendar createCalendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        // Calendar의 month는 0부터 시작 (예: Calendar.JANUARY는 0)
        calendar.set(year, month - 1, day); // month - 1 로 수정
        return calendar;
    }


}
