package com.sugyeong.churavel.region;

/**
 * 체크인할 수 있는 한 칸.
 * - 한국 시·군·구: code = 지역 코드(1101, DOKDO), province = 시·도, country = KR
 * - 다른 나라 행정구역: code = Natural Earth adm1_code(JPN-1860 = 도쿄도), province = null, country = JP
 * - 나라 하나 통째로: code = country = ISO alpha-2 (지역 지도가 없는 작은 나라, 또는 예전에 남긴 기록)
 * code 는 프론트 지도 데이터(regions.json / world.json)의 id 와 같다.
 */
public record Region(String code, String name, String province, String country) {

    public static final String KOREA = "KR";

    public boolean isCountry() {
        return code.equals(country);
    }
}
