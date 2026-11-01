package com.reshma.seatbooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String showName;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private String heldBy;

    private LocalDateTime holdExpiresAt;

    private String bookedBy;

    // Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE.
    // If another transaction changed this row first, the update fails instead of overwriting it.
    @Version
    private Long version;

    protected Seat() {
    }

    public Seat(String showName, String seatNumber) {
        this.showName = showName;
        this.seatNumber = seatNumber;
    }

    /** A seat can be taken if it is free, held by the same user, or its hold has expired. */
    public boolean isAvailableFor(String user, LocalDateTime now) {
        return switch (status) {
            case AVAILABLE -> true;
            case HELD -> user.equals(heldBy) || holdExpiresAt.isBefore(now);
            case BOOKED -> false;
        };
    }

    public void hold(String user, LocalDateTime until) {
        this.status = SeatStatus.HELD;
        this.heldBy = user;
        this.holdExpiresAt = until;
    }

    public void book(String user) {
        this.status = SeatStatus.BOOKED;
        this.bookedBy = user;
        this.heldBy = null;
        this.holdExpiresAt = null;
    }

    public Long getId() { return id; }
    public String getShowName() { return showName; }
    public String getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }
    public String getHeldBy() { return heldBy; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public String getBookedBy() { return bookedBy; }
    public Long getVersion() { return version; }
}
