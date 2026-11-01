package com.reshma.seatbooking;

import com.reshma.seatbooking.dto.BookingRequest;
import com.reshma.seatbooking.dto.BookingResponse;
import com.reshma.seatbooking.dto.LockStrategy;
import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.entity.SeatStatus;
import com.reshma.seatbooking.exception.SeatUnavailableException;
import com.reshma.seatbooking.repository.BookingRepository;
import com.reshma.seatbooking.repository.SeatRepository;
import com.reshma.seatbooking.service.BookingService;
import com.reshma.seatbooking.service.HoldExpiryScheduler;
import com.reshma.seatbooking.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BookingFeaturesTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private SeatService seatService;
    @Autowired
    private HoldExpiryScheduler holdExpiryScheduler;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @Test
    void groupBookingIsAllOrNothing() {
        Seat a1 = seatRepository.save(new Seat("Inception", "A1"));
        Seat a2 = seatRepository.save(new Seat("Inception", "A2"));
        Seat a3 = seatRepository.save(new Seat("Inception", "A3"));
        bookingService.book(new BookingRequest("req-1", "alice", List.of(a2.getId()), LockStrategy.OPTIMISTIC));

        // bob wants A1, A2, A3 together, but A2 is already taken
        assertThatThrownBy(() -> bookingService.book(
                new BookingRequest("req-2", "bob", List.of(a1.getId(), a2.getId(), a3.getId()), LockStrategy.PESSIMISTIC)))
                .isInstanceOf(SeatUnavailableException.class);

        // A1 and A3 must still be free: nothing was partially booked
        assertThat(seatRepository.findById(a1.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seatRepository.findById(a3.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void sameRequestIdDoesNotBookTwice() {
        Seat seat = seatRepository.save(new Seat("Inception", "A1"));
        BookingRequest request = new BookingRequest("checkout-42", "alice", List.of(seat.getId()), LockStrategy.OPTIMISTIC);

        BookingResponse first = bookingService.book(request);
        BookingResponse second = bookingService.book(request);   // user double-clicked "Pay"

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isTrue();
        assertThat(second.bookingId()).isEqualTo(first.bookingId());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void heldSeatIsBlockedForOthersButNotForTheHolder() {
        Seat seat = seatRepository.save(new Seat("Inception", "A1"));
        seatService.hold(seat.getId(), "alice");

        assertThatThrownBy(() -> seatService.hold(seat.getId(), "bob"))
                .isInstanceOf(SeatUnavailableException.class);
        assertThatThrownBy(() -> bookingService.book(
                new BookingRequest("req-bob", "bob", List.of(seat.getId()), LockStrategy.OPTIMISTIC)))
                .isInstanceOf(SeatUnavailableException.class);

        BookingResponse booked = bookingService.book(
                new BookingRequest("req-alice", "alice", List.of(seat.getId()), LockStrategy.OPTIMISTIC));
        assertThat(booked.user()).isEqualTo("alice");
    }

    @Test
    void expiredHoldIsReleased() {
        Seat seat = new Seat("Inception", "A1");
        seat.hold("alice", LocalDateTime.now().minusMinutes(1));   // hold that ended a minute ago
        seat = seatRepository.save(seat);

        int released = holdExpiryScheduler.releaseExpiredHolds();

        assertThat(released).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}
