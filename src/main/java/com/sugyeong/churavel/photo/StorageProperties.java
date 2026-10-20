package com.sugyeong.churavel.photo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** application.yml 의 churavel.storage.* */
@ConfigurationProperties(prefix = "churavel.storage")
public record StorageProperties(Path photoDir) {

    public StorageProperties {
        if (photoDir == null) {
            photoDir = Path.of("data", "photos");
        }
    }
}
