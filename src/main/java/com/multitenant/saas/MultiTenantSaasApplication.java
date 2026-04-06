package com.multitenant.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
@EnableCaching
public class MultiTenantSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantSaasApplication.class, args);
    }
}

