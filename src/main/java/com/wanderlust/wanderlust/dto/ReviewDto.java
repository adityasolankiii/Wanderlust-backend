package com.wanderlust.wanderlust.dto;

import com.wanderlust.wanderlust.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private String id;
    private String comment;
    private Integer rating;
    private UserDto author;
    private String listingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewDto from(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .comment(review.getComment())
                .rating(review.getRating())
                .author(UserDto.from(review.getAuthor()))
                .listingId(review.getListing().getId())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
