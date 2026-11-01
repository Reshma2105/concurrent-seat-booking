package com.reshma.seatbooking.repository;

import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowNameOrderBySeatNumber(String showName);

    // Pessimistic locking: runs SELECT ... FOR UPDATE, so other transactions wait
    // until this one commits. Rows are locked in id order so two group bookings
    // can never wait on each other forever (deadlock).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
    List<Seat> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
    List<Seat> findAllByIdOrdered(@Param("ids") Collection<Long> ids);

    // Bulk update does not go through @Version, so the version is bumped by hand.
    @Modifying
    @Query("""
            UPDATE Seat s
               SET s.status = :available, s.heldBy = null, s.holdExpiresAt = null, s.version = s.version + 1
             WHERE s.status = :held AND s.holdExpiresAt < :now
            """)
    int releaseExpiredHolds(@Param("now") LocalDateTime now,
                            @Param("held") SeatStatus held,
                            @Param("available") SeatStatus available);
}
