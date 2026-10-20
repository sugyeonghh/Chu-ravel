package com.sugyeong.churavel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChuravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChuravelApplication.class, args);
    }
}
