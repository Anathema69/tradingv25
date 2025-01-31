package com.nectum.tradingv25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TradingV25Application {
    public static void main(String[] args) {
        SpringApplication.run(TradingV25Application.class, args);
    }
}