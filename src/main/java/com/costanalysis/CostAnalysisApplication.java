package com.costanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CostAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(CostAnalysisApplication.class, args);
    }
}
