package com.reshma.seatbooking.dto;

import com.reshma.seatbooking.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param duplicate true when this request id was already processed and the original booking is returned
 */
public record BookingResponse(Long bookingId, String requestId, String user, List<Long> seatIds,
                              LocalDateTime createdAt, boolean duplicate) {

    public static BookingResponse from(Booking booking, boolean duplicate) {
        return new BookingResponse(booking.getId(), booking.getRequestId(), booking.getUserName(),
                List.copyOf(booking.getSeatIds()), booking.getCreatedAt(), duplicate);
    }
}
