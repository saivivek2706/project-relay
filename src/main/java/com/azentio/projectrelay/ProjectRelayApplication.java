package com.azentio.projectrelay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // Automatically registers @ConfigurationProperties classes as beans
public class ProjectRelayApplication {

    public static void main(String[] args) {

        SpringApplication.run(ProjectRelayApplication.class, args);
    }
}
