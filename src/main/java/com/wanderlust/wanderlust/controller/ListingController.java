package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<ListingsResponse> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        ListingsResponse response = listingService.getAll(
                category, minPrice, maxPrice, location, country, page, limit, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ListingsResponse> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(listingService.getByCategory(category, page, limit));
    }

    @GetMapping("/search")
    public ResponseEntity<ListingsResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(listingService.search(q, page, limit));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingDto> create(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Integer price,
            @RequestParam String location,
            @RequestParam String country,
            @RequestParam String category,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) String imageUrl,
            @AuthenticationPrincipal UserDetails userDetails) {
        ListingDto listing = listingService.create(
                title, description, price, location, country, category,
                images, imageUrl, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(listing);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingDto> update(
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String deleteImageIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        ListingDto listing = listingService.update(
                id, title, description, price, location, country, category,
                images, imageUrl, deleteImageIds, userDetails.getUsername());
        return ResponseEntity.ok(listing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        listingService.delete(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully"));
    }

    @PatchMapping("/{id}/toggle-reserved")
    public ResponseEntity<ListingDto> toggleReserved(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(listingService.toggleReserveStatus(id, userDetails.getUsername()));
    }
}
