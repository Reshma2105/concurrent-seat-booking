package com.reshma.seatbooking;

import com.reshma.seatbooking.dto.BookingRequest;
import com.reshma.seatbooking.dto.LockStrategy;
import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.entity.SeatStatus;
import com.reshma.seatbooking.repository.BookingRepository;
import com.reshma.seatbooking.repository.SeatRepository;
import com.reshma.seatbooking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The main proof of the project: 500 users try to book the SAME seat at the SAME moment.
 * Exactly one must succeed, with both locking strategies.
 */
@SpringBootTest
class ConcurrentBookingTest {

    private static final int USERS = 500;

    @Autowired
    private BookingService bookingService;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(LockStrategy.class)
    void onlyOneOf500UsersGetsTheSameSeat(LockStrategy strategy) throws InterruptedException {
        Seat seat = seatRepository.save(new Seat("Inception", "A1"));

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch ready = new CountDownLatch(USERS);   // every thread is waiting at the start line
        CountDownLatch start = new CountDownLatch(1);       // fires all threads together
        CountDownLatch done = new CountDownLatch(USERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        long begin = System.currentTimeMillis();
        for (int i = 0; i < USERS; i++) {
            String user = "user-" + i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.book(new BookingRequest(
                            UUID.randomUUID().toString(), user, List.of(seat.getId()), strategy));
                    success.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(2, TimeUnit.MINUTES)).isTrue();
        pool.shutdown();
        long millis = System.currentTimeMillis() - begin;

        System.out.printf("%s: %d users -> %d booked, %d rejected, %d ms%n",
                strategy, USERS, success.get(), rejected.get(), millis);

        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(USERS - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }
}
