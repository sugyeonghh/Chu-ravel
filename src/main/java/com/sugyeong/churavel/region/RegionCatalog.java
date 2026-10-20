package com.sugyeong.churavel.region;

import com.sugyeong.churavel.common.NotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 체크인할 수 있는 칸 목록은 바뀌지 않는 기준 데이터라 DB 대신 classpath 의 csv 에서 읽는다.
 * - regions.csv: 한국 시·군·구 (tools/map/gen.js)
 * - subdivisions.csv: 다른 나라의 1단계 행정구역 — 일본 도도부현, 미국 주 … (tools/map/gen-countries.mjs)
 * - countries.csv: 한국을 뺀 나라 (tools/map/gen-world.mjs). 지역 지도가 없는 작은 나라는 나라 하나로 체크인한다.
 * 세 코드는 겹치지 않는다 (한국은 숫자 4자리·DOKDO, 행정구역은 JPN-1860 꼴, 나라는 영문 2자리).
 */
@Component
public class RegionCatalog {

    private final Map<String, Region> regions;

    public RegionCatalog() {
        Map<String, Region> map = new LinkedHashMap<>();
        load("regions.csv", cols -> new Region(cols[0], cols[1], cols[2], Region.KOREA), map);
        load("subdivisions.csv", cols -> new Region(cols[0], cols[1], null, cols[2]), map);
        load("countries.csv", cols -> new Region(cols[0], cols[1], null, cols[0]), map);
        this.regions = Collections.unmodifiableMap(map);
    }

    private static void load(String path, Function<String[], Region> row, Map<String, Region> into) {
        var resource = new ClassPathResource(path);
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                Region r = row.apply(line.split(",", -1));
                into.put(r.code(), r);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 를 읽지 못했습니다", e);
        }
    }

    public Optional<Region> find(String code) {
        return Optional.ofNullable(regions.get(code));
    }

    public Region get(String code) {
        return find(code).orElseThrow(() -> new NotFoundException("없는 지역 코드예요: " + code));
    }

    public Collection<Region> all() {
        return regions.values();
    }

    public int size() {
        return regions.size();
    }
}
