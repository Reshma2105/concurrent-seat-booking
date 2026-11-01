package com.reshma.seatbooking.entity;

/**
 * Life cycle of a seat:
 * AVAILABLE -> HELD (user is at checkout, for a few minutes) -> BOOKED
 * A HELD seat goes back to AVAILABLE if the hold expires.
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
