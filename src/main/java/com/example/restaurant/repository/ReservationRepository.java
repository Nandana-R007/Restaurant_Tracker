package com.example.restaurant.repository;

import com.example.restaurant.entity.Reservation;
import com.example.restaurant.entity.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByTableIdAndReservationDate(Long tableId, LocalDate date);

    List<Reservation> findByStatusIn(Collection<ReservationStatus> statuses);   // "active" reservations

    boolean existsByTableId(Long tableId);

    /** The sitting currently at a table (status SEATED). */
    Optional<Reservation> findFirstByTableIdAndStatus(Long tableId, ReservationStatus status);

    /**
     * Overlap check:  existing.start < newEnd  AND  existing.end > newStart
     * (only for the statuses passed in, e.g. CONFIRMED and SEATED).
     */
    boolean existsByTableIdAndReservationDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long tableId, LocalDate date, Collection<ReservationStatus> statuses, LocalTime newEnd, LocalTime newStart);
}
