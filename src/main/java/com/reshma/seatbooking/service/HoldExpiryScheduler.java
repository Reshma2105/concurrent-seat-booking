package com.reshma.seatbooking.service;

import com.reshma.seatbooking.entity.SeatStatus;
import com.reshma.seatbooking.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Frees seats whose hold has expired (user left the checkout page).
 * Booking also treats an expired hold as available, so a seat is never stuck
 * even in the gap between two runs of this job.
 */
@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final SeatRepository seatRepository;

    public HoldExpiryScheduler(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Scheduled(fixedRateString = "${booking.hold-release-interval-ms:30000}")
    @Transactional
    public int releaseExpiredHolds() {
        int released = seatRepository.releaseExpiredHolds(LocalDateTime.now(), SeatStatus.HELD, SeatStatus.AVAILABLE);
        if (released > 0) {
            log.info("Released {} expired seat hold(s)", released);
        }
        return released;
    }
}
