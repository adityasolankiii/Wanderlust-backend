package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.service.ReserveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReserveController {

    private final ReserveService reserveService;

    @GetMapping("/api/reserves")
    public ResponseEntity<ReservesResponse> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reserveService.getMyReservations(userDetails.getUsername(), page, limit));
    }

    @GetMapping("/api/reserves/{id}")
    public ResponseEntity<ReserveDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(reserveService.getById(id));
    }

    @GetMapping("/api/users/{userId}/reserves")
    public ResponseEntity<ReservesResponse> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reserveService.getByUser(userId, page, limit));
    }

    @GetMapping("/api/listings/{listingId}/reserves")
    public ResponseEntity<ReservesResponse> getByListing(
            @PathVariable String listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reserveService.getByListing(listingId, page, limit));
    }

    @PostMapping("/api/reserves")
    public ResponseEntity<ReserveDto> create(
            @Valid @RequestBody ReserveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReserveDto reserve = reserveService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(reserve);
    }

    @PostMapping("/api/reserves/{id}/cancel")
    public ResponseEntity<ReserveDto> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(reserveService.cancel(id, userDetails.getUsername(), reason));
    }

    @DeleteMapping("/api/reserves/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reserveService.delete(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Reservation deleted successfully"));
    }

    @PostMapping("/api/reserves/calculate-price")
    public ResponseEntity<Map<String, Object>> calculatePrice(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(reserveService.calculatePrice(
                (String) request.get("listingId"),
                (String) request.get("checkin"),
                (String) request.get("checkout"),
                (int) request.get("adult"),
                (int) request.get("children")
        ));
    }

    @GetMapping("/api/listings/{listingId}/booked-dates")
    public ResponseEntity<List<Map<String, String>>> getBookedDates(@PathVariable String listingId) {
        return ResponseEntity.ok(reserveService.getBookedDates(listingId));
    }
}
