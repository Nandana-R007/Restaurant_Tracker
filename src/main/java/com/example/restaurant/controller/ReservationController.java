package com.example.restaurant.controller;

import com.example.restaurant.dto.ReservationRequest;
import com.example.restaurant.dto.ReservationStatusRequest;
import com.example.restaurant.dto.Responses.ReservationResponse;
import com.example.restaurant.dto.WalkInRequest;
import com.example.restaurant.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) { this.reservationService = reservationService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) { return reservationService.create(request); }

    @PostMapping("/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse walkIn(@Valid @RequestBody WalkInRequest request) { return reservationService.walkIn(request); }

    /** Optional filters (both needed): /api/reservations?tableId=2&date=2026-09-28 */
    @GetMapping
    public List<ReservationResponse> getAll(@RequestParam(required = false) Long tableId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getAll(tableId, date);
    }

    @GetMapping("/active")
    public List<ReservationResponse> getActive() { return reservationService.getActive(); }

    @GetMapping("/{id}")
    public ReservationResponse getOne(@PathVariable Long id) { return reservationService.getById(id); }

    @PutMapping("/{id}/status")
    public ReservationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ReservationStatusRequest request) {
        return reservationService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) { reservationService.cancel(id); }
}
