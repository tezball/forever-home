package com.example.foreverhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ForeverHomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForeverHomeApplication.class, args);
    }

}
