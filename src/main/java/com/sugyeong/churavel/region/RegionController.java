package com.sugyeong.churavel.region;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@Tag(name = "공개", description = "로그인 없이 쓰는 API")
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionCatalog catalog;

    public RegionController(RegionCatalog catalog) {
        this.catalog = catalog;
    }

    @Operation(summary = "지역·나라 코드표")
    @ApiResponse(responseCode = "200", description = "한국 시·군·구 + 다른 나라 지역 + 나라")
    @GetMapping
    public Collection<Region> list() {
        return catalog.all();
    }
}
