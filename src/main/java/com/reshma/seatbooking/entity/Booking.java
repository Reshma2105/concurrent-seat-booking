package com.reshma.seatbooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sent by the client with every booking attempt. The unique constraint guarantees
    // that a double click or a network retry can never create a second booking.
    @Column(nullable = false, unique = true)
    private String requestId;

    private String userName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seat", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    private List<Long> seatIds = new ArrayList<>();

    private LocalDateTime createdAt;

    protected Booking() {
    }

    public Booking(String requestId, String userName, List<Long> seatIds) {
        this.requestId = requestId;
        this.userName = userName;
        this.seatIds = new ArrayList<>(seatIds);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRequestId() { return requestId; }
    public String getUserName() { return userName; }
    public List<Long> getSeatIds() { return seatIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
