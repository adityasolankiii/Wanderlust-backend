package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    List<Review> findByListingIdOrderByCreatedAtDesc(String listingId);

    List<Review> findByAuthorId(String authorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listing.id = :listingId")
    Double getAverageRatingByListingId(@Param("listingId") String listingId);

    long countByListingId(String listingId);
}
