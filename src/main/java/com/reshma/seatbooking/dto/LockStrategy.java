package com.reshma.seatbooking.dto;

public enum LockStrategy {
    /** Read freely, detect conflicts at commit using the version column. Best when conflicts are rare. */
    OPTIMISTIC,
    /** Lock the rows while reading so others wait. Best when many users fight for the same seats. */
    PESSIMISTIC
}
