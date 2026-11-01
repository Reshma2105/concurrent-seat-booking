package com.reshma.seatbooking.service;

import com.reshma.seatbooking.dto.BookingRequest;
import com.reshma.seatbooking.dto.BookingResponse;
import com.reshma.seatbooking.dto.LockStrategy;
import com.reshma.seatbooking.exception.SeatUnavailableException;
import com.reshma.seatbooking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    static final int MAX_ATTEMPTS = 3;

    private final BookingTransactionService transactionService;
    private final BookingRepository bookingRepository;

    public BookingService(BookingTransactionService transactionService, BookingRepository bookingRepository) {
        this.transactionService = transactionService;
        this.bookingRepository = bookingRepository;
    }

    public BookingResponse book(BookingRequest request) {
        validate(request);
        List<Long> seatIds = request.seatIds().stream().distinct().sorted().toList();
        LockStrategy strategy = request.strategy() == null ? LockStrategy.OPTIMISTIC : request.strategy();

        // 1. Duplicate click / network retry: return the booking that already exists
        Optional<BookingResponse> existing = findExisting(request.requestId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Try to book; retry a few times if another user changed the same seats at the same moment
        for (int attempt = 1; ; attempt++) {
            try {
                return BookingResponse.from(
                        transactionService.bookOnce(request.requestId(), request.user(), seatIds, strategy), false);
            } catch (OptimisticLockingFailureException e) {
                log.debug("Optimistic lock conflict for request {} (attempt {})", request.requestId(), attempt);
                if (attempt == MAX_ATTEMPTS) {
                    throw new SeatUnavailableException("Seats are being booked by other users, please try again");
                }
            } catch (DataIntegrityViolationException | SeatUnavailableException e) {
                // The same requestId may have been booked by a parallel duplicate request
                return findExisting(request.requestId()).orElseThrow(() -> e);
            }
        }
    }

    private Optional<BookingResponse> findExisting(String requestId) {
        return bookingRepository.findByRequestId(requestId).map(b -> BookingResponse.from(b, true));
    }

    private void validate(BookingRequest request) {
        if (request.requestId() == null || request.requestId().isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (request.user() == null || request.user().isBlank()) {
            throw new IllegalArgumentException("user is required");
        }
        if (request.seatIds() == null || request.seatIds().isEmpty()) {
            throw new IllegalArgumentException("at least one seat must be selected");
        }
    }
}
