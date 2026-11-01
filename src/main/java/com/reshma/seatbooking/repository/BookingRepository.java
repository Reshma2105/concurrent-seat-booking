package com.reshma.seatbooking.repository;

import com.reshma.seatbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);
}
