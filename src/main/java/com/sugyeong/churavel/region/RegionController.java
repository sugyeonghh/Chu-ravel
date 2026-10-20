package com.sugyeong.churavel.region;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionCatalog catalog;

    public RegionController(RegionCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public Collection<Region> list() {
        return catalog.all();
    }
}
