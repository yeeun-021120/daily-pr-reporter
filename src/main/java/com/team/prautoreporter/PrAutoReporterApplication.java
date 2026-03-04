package com.team.prautoreporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PrAutoReporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrAutoReporterApplication.class, args);
    }
}