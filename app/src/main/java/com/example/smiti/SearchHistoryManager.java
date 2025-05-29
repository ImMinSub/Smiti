package com.example.smiti; // 자신의 패키지명으로 변경하세요

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREFS_NAME = "RecentSearchPrefsSmiti"; // 앱별 고유한 이름 사용 권장
    private static final String KEY_RECENT_SEARCHES = "recent_searches_list";
    private static final int MAX_HISTORY_SIZE = 10; // 최근 검색어 최대 저장 개수

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<String> getRecentSearches() {
        String json = sharedPreferences.getString(KEY_RECENT_SEARCHES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> searches = gson.fromJson(json, type);
        return searches != null ? searches : new ArrayList<>();
    }

    public void addSearchTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return;
        }
        term = term.trim();
        List<String> searches = getRecentSearches();

        // 기존에 있다면 제거 (최신화를 위해)
        searches.remove(term);
        // 맨 앞에 추가
        searches.add(0, term);

        // 최대 개수 제한
        if (searches.size() > MAX_HISTORY_SIZE) {
            searches = searches.subList(0, MAX_HISTORY_SIZE);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_RECENT_SEARCHES, gson.toJson(searches));
        editor.apply();
    }

    public void removeSearchTerm(String term) {
        List<String> searches = getRecentSearches();
        boolean removed = searches.remove(term);
        if (removed) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_RECENT_SEARCHES, gson.toJson(searches));
            editor.apply();
        }
    }

    public void clearAllRecentSearches() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_RECENT_SEARCHES);
        editor.apply();
    }
}
