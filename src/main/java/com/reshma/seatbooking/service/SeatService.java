package com.reshma.seatbooking.service;

import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.exception.NotFoundException;
import com.reshma.seatbooking.exception.SeatUnavailableException;
import com.reshma.seatbooking.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final long holdMinutes;

    public SeatService(SeatRepository seatRepository, @Value("${booking.hold-minutes:5}") long holdMinutes) {
        this.seatRepository = seatRepository;
        this.holdMinutes = holdMinutes;
    }

    public List<Seat> list(String showName) {
        return showName == null ? seatRepository.findAll() : seatRepository.findByShowNameOrderBySeatNumber(showName);
    }

    public Seat get(Long id) {
        return seatRepository.findById(id).orElseThrow(() -> new NotFoundException("Seat " + id + " not found"));
    }

    /**
     * Reserves a seat for a few minutes while the user completes checkout.
     * Uses optimistic locking: if two users hold the same seat at once, one gets a 409.
     */
    @Transactional
    public Seat hold(Long id, String user) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user is required");
        }
        Seat seat = get(id);
        LocalDateTime now = LocalDateTime.now();
        if (!seat.isAvailableFor(user, now)) {
            throw new SeatUnavailableException("Seat " + seat.getSeatNumber() + " is not available");
        }
        seat.hold(user, now.plusMinutes(holdMinutes));
        return seatRepository.saveAndFlush(seat);
    }
}
