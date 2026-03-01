package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, String> {
    List<ListingImage> findByListingId(String listingId);
    void deleteByListingId(String listingId);
}
