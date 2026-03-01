package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/listings/{listingId}/reviews")
    public ResponseEntity<ReviewsResponse> getByListing(@PathVariable String listingId) {
        return ResponseEntity.ok(reviewService.getByListing(listingId));
    }

    @GetMapping("/api/listings/{listingId}/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> getById(
            @PathVariable String listingId,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.getById(listingId, reviewId));
    }

    @GetMapping("/api/users/{userId}/reviews")
    public ResponseEntity<ReviewsResponse> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(reviewService.getByUser(userId));
    }

    @PostMapping("/api/listings/{listingId}/reviews")
    public ResponseEntity<ReviewDto> create(
            @PathVariable String listingId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewDto review = reviewService.create(listingId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PutMapping("/api/listings/{listingId}/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> update(
            @PathVariable String listingId,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.update(listingId, reviewId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/api/listings/{listingId}/reviews/{reviewId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String listingId,
            @PathVariable String reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.delete(listingId, reviewId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}
