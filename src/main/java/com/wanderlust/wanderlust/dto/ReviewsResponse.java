package com.wanderlust.wanderlust.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewsResponse {
    private List<ReviewDto> reviews;
    private long total;
    private double averageRating;
}
