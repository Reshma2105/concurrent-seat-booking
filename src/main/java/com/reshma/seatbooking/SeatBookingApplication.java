package com.reshma.seatbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // needed for the job that releases expired seat holds
public class SeatBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatBookingApplication.class, args);
    }
}
