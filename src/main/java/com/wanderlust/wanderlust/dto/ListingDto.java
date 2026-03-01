package com.wanderlust.wanderlust.dto;

import com.wanderlust.wanderlust.entity.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {
    private String id;
    private String title;
    private String description;
    private Map<String, String> image; // Primary image {url, filename} — backward compat
    private List<Map<String, String>> images; // All images [{url, filename}, ...]
    private Integer price;
    private String location;
    private String country;
    private String category;
    private Boolean isReserved;
    private UserDto owner;
    private List<ReviewDto> reviews;
    private Map<String, Object> geometry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ListingDto from(Listing listing) {
        // Build images list from the new images collection
        List<Map<String, String>> imagesList = new java.util.ArrayList<>();

        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            for (var img : listing.getImages()) {
                imagesList.add(Map.of(
                        "id", img.getId() != null ? img.getId() : "",
                        "url", img.getUrl() != null ? img.getUrl() : "",
                        "filename", img.getFilename() != null ? img.getFilename() : ""
                ));
            }
        }

        // Fallback to legacy single image field if no images in the new table
        if (imagesList.isEmpty() && listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            imagesList.add(Map.of(
                    "url", listing.getImageUrl(),
                    "filename", listing.getImageFilename() != null ? listing.getImageFilename() : ""
            ));
        }

        // Primary image is the first in the list (backward compatibility)
        Map<String, String> primaryImage = imagesList.isEmpty()
                ? Map.of("url", "", "filename", "")
                : imagesList.get(0);

        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .image(primaryImage)
                .images(imagesList)
                .price(listing.getPrice())
                .location(listing.getLocation())
                .country(listing.getCountry())
                .category(listing.getCategory())
                .isReserved(listing.getIsReserved())
                .owner(UserDto.from(listing.getOwner()))
                .reviews(listing.getReviews() != null
                        ? listing.getReviews().stream().map(ReviewDto::from).toList()
                        : List.of())
                .geometry(Map.of(
                        "type", "Point",
                        "coordinates", List.of(
                                listing.getLongitude() != null ? listing.getLongitude() : 0.0,
                                listing.getLatitude() != null ? listing.getLatitude() : 0.0
                        )
                ))
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
