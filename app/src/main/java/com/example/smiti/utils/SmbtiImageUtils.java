package com.example.smiti.utils;

import com.example.smiti.R;

import java.util.HashMap;
import java.util.Map;


// SMBTI 타입에 따른 프로필 이미지 리소스를 매핑하는 유틸리티 클래스
public class SmbtiImageUtils {
    
    private static final Map<String, Integer> smbtiImageMap = new HashMap<>();
    
    static {
        // SMBTI 타입과 이미지 리소스 매핑
        smbtiImageMap.put("EIPM", R.drawable.eipm);
        smbtiImageMap.put("EIPD", R.drawable.eipd);
        smbtiImageMap.put("EIFM", R.drawable.eifm);
        smbtiImageMap.put("EIFD", R.drawable.eifd);
        smbtiImageMap.put("ECPM", R.drawable.ecpm);
        smbtiImageMap.put("ECPD", R.drawable.ecpd);
        smbtiImageMap.put("ECFM", R.drawable.ecfm);
        smbtiImageMap.put("ECFD", R.drawable.ecfd);
        smbtiImageMap.put("TIPM", R.drawable.tipm);
        smbtiImageMap.put("TIPD", R.drawable.tipd);
        smbtiImageMap.put("TIFM", R.drawable.tifm);
        smbtiImageMap.put("TIFD", R.drawable.tifd);
        smbtiImageMap.put("TCPM", R.drawable.tcpm);
        smbtiImageMap.put("TCPD", R.drawable.tcpd);
        smbtiImageMap.put("TCFM", R.drawable.tcfm);
        smbtiImageMap.put("TCFD", R.drawable.tcfd);
    }
    
    /**
     * SMBTI 타입에 맞는 프로필 이미지 리소스 ID를 반환
     * @param smbtiType SMBTI 타입 문자열 (예: "EIPM", "TCFD")
     * @return 해당하는 이미지 리소스 ID, 매칭되지 않으면 기본 플레이스홀더 이미지
     */
    public static int getProfileImageResource(String smbtiType) {
        if (smbtiType == null || smbtiType.trim().isEmpty()) {
            return R.drawable.ic_profile_placeholder;
        }
        
        // 대소문자 무관하게 처리
        String normalizedType = smbtiType.trim().toUpperCase();
        
        Integer resourceId = smbtiImageMap.get(normalizedType);
        
        // 매칭되는 이미지가 있으면 해당 리소스 반환, 없으면 기본 이미지
        return resourceId != null ? resourceId : R.drawable.ic_profile_placeholder;
    }
    
    /**
     * SMBTI 타입이 유효한지 확인
     * @param smbtiType 확인할 SMBTI 타입
     * @return 유효하면 true, 아니면 false
     */
    public static boolean isValidSmbtiType(String smbtiType) {
        if (smbtiType == null || smbtiType.trim().isEmpty()) {
            return false;
        }
        return smbtiImageMap.containsKey(smbtiType.trim().toUpperCase());
    }
}
