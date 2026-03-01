package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, String> {

    Page<Listing> findByCategory(String category, Pageable pageable);

    Page<Listing> findByOwnerId(String ownerId, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE " +
            "LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Listing> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE " +
            "(:category IS NULL OR l.category = :category) AND " +
            "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR l.price <= :maxPrice) AND " +
            "(:location IS NULL OR LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:country IS NULL OR LOWER(l.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    Page<Listing> findWithFilters(
            @Param("category") String category,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("location") String location,
            @Param("country") String country,
            Pageable pageable);
}
