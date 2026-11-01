package com.reshma.seatbooking.controller;

import com.reshma.seatbooking.dto.BookingRequest;
import com.reshma.seatbooking.dto.BookingResponse;
import com.reshma.seatbooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** 201 Created for a new booking, 200 OK when the same requestId was already booked. */
    @PostMapping
    public ResponseEntity<BookingResponse> book(@RequestBody BookingRequest request) {
        BookingResponse response = bookingService.book(request);
        return ResponseEntity.status(response.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }
}
