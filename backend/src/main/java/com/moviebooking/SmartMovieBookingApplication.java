package com.moviebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartMovieBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMovieBookingApplication.class, args);
    }
}
