package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.Listing;
import com.wanderlust.wanderlust.entity.Review;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.exception.ResourceNotFoundException;
import com.wanderlust.wanderlust.exception.UnauthorizedAccessException;
import com.wanderlust.wanderlust.repository.ListingRepository;
import com.wanderlust.wanderlust.repository.ReviewRepository;
import com.wanderlust.wanderlust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ReviewsResponse getByListing(String listingId) {
        List<Review> reviews = reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId);
        Double avgRating = reviewRepository.getAverageRatingByListingId(listingId);

        return ReviewsResponse.builder()
                .reviews(reviews.stream().map(ReviewDto::from).toList())
                .total(reviews.size())
                .averageRating(avgRating != null ? avgRating : 0.0)
                .build();
    }

    public ReviewDto getById(String listingId, String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getListing().getId().equals(listingId)) {
            throw new ResourceNotFoundException("Review not found for this listing");
        }
        return ReviewDto.from(review);
    }

    public ReviewsResponse getByUser(String userId) {
        List<Review> reviews = reviewRepository.findByAuthorId(userId);
        return ReviewsResponse.builder()
                .reviews(reviews.stream().map(ReviewDto::from).toList())
                .total(reviews.size())
                .averageRating(0.0)
                .build();
    }

    @Transactional
    public ReviewDto create(String listingId, ReviewRequest request, String username) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = Review.builder()
                .comment(request.getComment())
                .rating(request.getRating())
                .author(author)
                .listing(listing)
                .build();

        review = reviewRepository.save(review);
        return ReviewDto.from(review);
    }

    @Transactional
    public ReviewDto update(String listingId, String reviewId, ReviewRequest request, String username) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getListing().getId().equals(listingId)) {
            throw new ResourceNotFoundException("Review not found for this listing");
        }
        if (!review.getAuthor().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to update this review");
        }

        if (request.getComment() != null) review.setComment(request.getComment());
        if (request.getRating() != null) review.setRating(request.getRating());

        review = reviewRepository.save(review);
        return ReviewDto.from(review);
    }

    @Transactional
    public void delete(String listingId, String reviewId, String username) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getListing().getId().equals(listingId)) {
            throw new ResourceNotFoundException("Review not found for this listing");
        }

        // Allow the review author OR listing owner to delete
        Listing listing = review.getListing();
        if (!review.getAuthor().getUsername().equals(username)
                && !listing.getOwner().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
    }
}
