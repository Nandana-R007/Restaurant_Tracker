package com.example.restaurant.service;

import com.example.restaurant.dto.ReservationRequest;
import com.example.restaurant.dto.Responses.ReservationResponse;
import com.example.restaurant.dto.WalkInRequest;
import com.example.restaurant.entity.*;
import com.example.restaurant.exception.BusinessRuleException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.TableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {
    // Only these reservations block a time slot. CANCELLED / COMPLETED do not.
    private static final List<ReservationStatus> BLOCKING = List.of(ReservationStatus.CONFIRMED, ReservationStatus.SEATED);

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;

    public ReservationService(ReservationRepository reservationRepository, TableRepository tableRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
    }

    /** Book a table for a future time slot. */
    public ReservationResponse create(ReservationRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BusinessRuleException("Start time must be before end time.");
        }
        if (req.reservationDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Reservation date cannot be in the past.");
        }
        RestaurantTable table = findTable(req.tableId());
        checkCapacity(table, req.partySize());
        checkNoOverlap(table, req.reservationDate(), req.startTime(), req.endTime());

        Reservation r = build(req.customerName(), req.phone(), req.partySize(), table,
                req.reservationDate(), req.startTime(), req.endTime(), ReservationStatus.CONFIRMED);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    /** Customer arrives without booking: table must be FREE, then it is seated immediately. */
    public ReservationResponse walkIn(WalkInRequest req) {
        RestaurantTable table = findTable(req.tableId());
        checkFree(table);
        checkCapacity(table, req.partySize());

        LocalTime start = LocalTime.now().withSecond(0).withNano(0);
        LocalTime end = start.plusHours(1);
        if (end.isBefore(start)) end = LocalTime.of(23, 59);   // do not wrap past midnight
        checkNoOverlap(table, LocalDate.now(), start, end);

        Reservation r = build(req.customerName(), req.phone(), req.partySize(), table,
                LocalDate.now(), start, end, ReservationStatus.SEATED);
        table.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    public List<ReservationResponse> getAll(Long tableId, LocalDate date) {
        List<Reservation> list = (tableId != null && date != null)
                ? reservationRepository.findByTableIdAndReservationDate(tableId, date)
                : reservationRepository.findAll();
        return list.stream().map(ReservationResponse::from).toList();
    }

    public List<ReservationResponse> getActive() {
        return reservationRepository.findByStatusIn(BLOCKING).stream().map(ReservationResponse::from).toList();
    }

    public ReservationResponse getById(Long id) { return ReservationResponse.from(findReservation(id)); }

    /** CONFIRMED->SEATED (table becomes OCCUPIED), CONFIRMED->CANCELLED, SEATED->COMPLETED (table FREE). */
    public ReservationResponse updateStatus(Long id, ReservationStatus next) {
        Reservation r = findReservation(id);
        RestaurantTable table = r.getTable();
        ReservationStatus current = r.getStatus();

        if (current == ReservationStatus.CONFIRMED && next == ReservationStatus.SEATED) {
            checkFree(table);                                   // Rule 1
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        } else if (current == ReservationStatus.CONFIRMED && next == ReservationStatus.CANCELLED) {
            // nothing else to do
        } else if (current == ReservationStatus.SEATED && next == ReservationStatus.COMPLETED) {
            table.setStatus(TableStatus.FREE);
            tableRepository.save(table);
        } else {
            throw new BusinessRuleException("Cannot change reservation status from " + current + " to " + next + ".");
        }
        r.setStatus(next);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    /** DELETE = cancel. Only a CONFIRMED (not yet seated) reservation can be cancelled. */
    public void cancel(Long id) {
        Reservation r = findReservation(id);
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED reservations can be cancelled (this one is " + r.getStatus() + ").");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    // ---------------- business rules ----------------

    /** Rule 1: table must be FREE. */
    private void checkFree(RestaurantTable table) {
        if (table.getStatus() != TableStatus.FREE) {
            throw new BusinessRuleException("Table " + table.getTableNumber() + " is currently occupied.");
        }
    }

    /** Rule 3: capacity must be enough for the party. */
    private void checkCapacity(RestaurantTable table, int partySize) {
        if (table.getCapacity() < partySize) {
            throw new BusinessRuleException("Table capacity is insufficient for this party size.");
        }
    }

    /** Rule 2: existing.start < newEnd AND existing.end > newStart, only for CONFIRMED/SEATED. */
    private void checkNoOverlap(RestaurantTable table, LocalDate date, LocalTime newStart, LocalTime newEnd) {
        boolean overlap = reservationRepository
                .existsByTableIdAndReservationDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        table.getId(), date, BLOCKING, newEnd, newStart);
        if (overlap) {
            throw new BusinessRuleException("Table " + table.getTableNumber()
                    + " already has a reservation that overlaps this time window.");
        }
    }

    private Reservation build(String name, String phone, int party, RestaurantTable table,
                              LocalDate date, LocalTime start, LocalTime end, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setCustomerName(name);
        r.setPhone(phone);
        r.setPartySize(party);
        r.setTable(table);
        r.setReservationDate(date);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setStatus(status);
        return r;
    }

    private RestaurantTable findTable(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table with ID " + id + " not found"));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + id + " not found"));
    }
}
