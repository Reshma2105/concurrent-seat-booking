package com.reshma.seatbooking.service;

import com.reshma.seatbooking.dto.LockStrategy;
import com.reshma.seatbooking.entity.Booking;
import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.exception.NotFoundException;
import com.reshma.seatbooking.exception.SeatUnavailableException;
import com.reshma.seatbooking.repository.BookingRepository;
import com.reshma.seatbooking.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One booking attempt inside one database transaction.
 * Kept in its own class so that BookingService can retry it: Spring's @Transactional
 * only works when the method is called from another bean, not from inside the same class.
 */
@Service
public class BookingTransactionService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    public BookingTransactionService(SeatRepository seatRepository, BookingRepository bookingRepository) {
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * All-or-nothing: if any seat is unavailable an exception is thrown and the
     * whole transaction rolls back, so no seat in the group gets booked.
     */
    @Transactional
    public Booking bookOnce(String requestId, String user, List<Long> seatIds, LockStrategy strategy) {
        List<Seat> seats = strategy == LockStrategy.PESSIMISTIC
                ? seatRepository.findAllByIdForUpdate(seatIds)
                : seatRepository.findAllByIdOrdered(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("One or more seats do not exist: " + seatIds);
        }

        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            if (!seat.isAvailableFor(user, now)) {
                throw new SeatUnavailableException("Seat " + seat.getSeatNumber() + " is not available");
            }
        }

        seats.forEach(seat -> seat.book(user));
        // Flush now so an optimistic-lock conflict shows up here, where the caller can retry it
        seatRepository.saveAllAndFlush(seats);

        return bookingRepository.saveAndFlush(new Booking(requestId, user, seatIds));
    }
}
