package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.Listing;
import com.wanderlust.wanderlust.entity.ListingImage;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.exception.ResourceNotFoundException;
import com.wanderlust.wanderlust.exception.UnauthorizedAccessException;
import com.wanderlust.wanderlust.repository.ListingRepository;
import com.wanderlust.wanderlust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final MapboxService mapboxService;

    @Transactional(readOnly = true)
    public ListingsResponse getAll(String category, Integer minPrice, Integer maxPrice,
                                    String location, String country,
                                    int page, int limit, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, limit, sort);

        Page<Listing> listingsPage = listingRepository.findWithFilters(
                category, minPrice, maxPrice, location, country, pageable);

        return ListingsResponse.builder()
                .listings(listingsPage.getContent().stream().map(ListingDto::from).toList())
                .total(listingsPage.getTotalElements())
                .page(page)
                .totalPages(listingsPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ListingDto getById(String id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
        return ListingDto.from(listing);
    }

    @Transactional(readOnly = true)
    public ListingsResponse getByCategory(String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Listing> listingsPage = listingRepository.findByCategory(category, pageable);

        return ListingsResponse.builder()
                .listings(listingsPage.getContent().stream().map(ListingDto::from).toList())
                .total(listingsPage.getTotalElements())
                .page(page)
                .totalPages(listingsPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ListingsResponse getByUser(String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Listing> listingsPage = listingRepository.findByOwnerId(userId, pageable);

        return ListingsResponse.builder()
                .listings(listingsPage.getContent().stream().map(ListingDto::from).toList())
                .total(listingsPage.getTotalElements())
                .page(page)
                .totalPages(listingsPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ListingsResponse search(String query, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Listing> listingsPage = listingRepository.search(query, pageable);

        return ListingsResponse.builder()
                .listings(listingsPage.getContent().stream().map(ListingDto::from).toList())
                .total(listingsPage.getTotalElements())
                .page(page)
                .totalPages(listingsPage.getTotalPages())
                .build();
    }

    @Transactional
    public ListingDto create(String title, String description, Integer price,
                              String location, String country, String category,
                              List<MultipartFile> images, String imageUrl, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = Listing.builder()
                .title(title)
                .description(description)
                .price(price)
                .location(location)
                .country(country)
                .category(category)
                .owner(owner)
                .build();

        // Handle multiple image uploads
        boolean hasUploadedImages = false;
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    Map<String, String> uploadResult = cloudinaryService.uploadImage(image);
                    ListingImage listingImage = ListingImage.builder()
                            .url(uploadResult.get("url"))
                            .filename(uploadResult.get("filename"))
                            .listing(listing)
                            .build();
                    listing.getImages().add(listingImage);
                    hasUploadedImages = true;

                    // Also set the legacy single image field to the first uploaded image
                    if (listing.getImageUrl() == null) {
                        listing.setImageUrl(uploadResult.get("url"));
                        listing.setImageFilename(uploadResult.get("filename"));
                    }
                }
            }
        }

        if (!hasUploadedImages) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                listing.setImageUrl(imageUrl);
                listing.setImageFilename("");
                ListingImage listingImage = ListingImage.builder()
                        .url(imageUrl)
                        .filename("")
                        .listing(listing)
                        .build();
                listing.getImages().add(listingImage);
            } else {
                String defaultUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800";
                listing.setImageUrl(defaultUrl);
                listing.setImageFilename("");
                ListingImage listingImage = ListingImage.builder()
                        .url(defaultUrl)
                        .filename("")
                        .listing(listing)
                        .build();
                listing.getImages().add(listingImage);
            }
        }

        // Geocode location
        double[] coordinates = mapboxService.geocode(location + ", " + country);
        if (coordinates != null) {
            listing.setLongitude(coordinates[0]);
            listing.setLatitude(coordinates[1]);
        }

        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }

    @Transactional
    public ListingDto update(String id, String title, String description, Integer price,
                              String location, String country, String category,
                              List<MultipartFile> images, String imageUrl, String deleteImageIds,
                              String username) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        if (!listing.getOwner().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to edit this listing");
        }

        if (title != null) listing.setTitle(title);
        if (description != null) listing.setDescription(description);
        if (price != null) listing.setPrice(price);
        if (location != null) listing.setLocation(location);
        if (country != null) listing.setCountry(country);
        if (category != null) listing.setCategory(category);

        // Delete specified images
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            String[] idsToDelete = deleteImageIds.split(",");
            for (String imgId : idsToDelete) {
                String trimmedId = imgId.trim();
                listing.getImages().removeIf(img -> {
                    if (img.getId() != null && img.getId().equals(trimmedId)) {
                        if (img.getFilename() != null && !img.getFilename().isEmpty()) {
                            cloudinaryService.deleteImage(img.getFilename());
                        }
                        return true;
                    }
                    return false;
                });
            }
        }

        // Upload new images
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    Map<String, String> uploadResult = cloudinaryService.uploadImage(image);
                    ListingImage listingImage = ListingImage.builder()
                            .url(uploadResult.get("url"))
                            .filename(uploadResult.get("filename"))
                            .listing(listing)
                            .build();
                    listing.getImages().add(listingImage);
                }
            }
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            ListingImage listingImage = ListingImage.builder()
                    .url(imageUrl)
                    .filename("")
                    .listing(listing)
                    .build();
            listing.getImages().add(listingImage);
        }

        // Update the legacy single-image field to match the first image
        if (!listing.getImages().isEmpty()) {
            ListingImage first = listing.getImages().get(0);
            listing.setImageUrl(first.getUrl());
            listing.setImageFilename(first.getFilename());
        }

        // Re-geocode if location changed
        if (location != null || country != null) {
            String geocodeQuery = (location != null ? location : listing.getLocation()) + ", "
                    + (country != null ? country : listing.getCountry());
            double[] coordinates = mapboxService.geocode(geocodeQuery);
            if (coordinates != null) {
                listing.setLongitude(coordinates[0]);
                listing.setLatitude(coordinates[1]);
            }
        }

        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }

    @Transactional
    public void delete(String id, String username) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        if (!listing.getOwner().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this listing");
        }

        // Delete all images from Cloudinary
        if (listing.getImages() != null) {
            for (ListingImage img : listing.getImages()) {
                if (img.getFilename() != null && !img.getFilename().isEmpty()) {
                    cloudinaryService.deleteImage(img.getFilename());
                }
            }
        }

        listingRepository.delete(listing);
    }

    @Transactional
    public ListingDto toggleReserveStatus(String id, String username) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        if (!listing.getOwner().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to update this listing");
        }

        listing.setIsReserved(!listing.getIsReserved());
        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }
}
