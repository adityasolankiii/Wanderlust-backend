package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.ListingsResponse;
import com.wanderlust.wanderlust.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ListingService listingService;

    @GetMapping("/{userId}/listings")
    public ResponseEntity<ListingsResponse> getUserListings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(listingService.getByUser(userId, page, limit));
    }
}
