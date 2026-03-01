package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.Reserve;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReserveRepository extends JpaRepository<Reserve, String> {

    Page<Reserve> findByReservedByIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Reserve> findByListingIdOrderByCreatedAtDesc(String listingId, Pageable pageable);

    List<Reserve> findByReservedById(String userId);

    List<Reserve> findByListingId(String listingId);

    /**
     * Find active, confirmed (non-cancelled) reservations for a listing that overlap with the given date range.
     * Overlap condition: existing.checkin < requestedCheckout AND existing.checkout > requestedCheckin
     */
    @Query("SELECT r FROM Reserve r WHERE r.listing.id = :listingId " +
           "AND r.isCancelled = false " +
           "AND r.status = 'confirmed' " +
           "AND r.checkin < :checkout AND r.checkout > :checkin")
    List<Reserve> findOverlappingReservations(
            @Param("listingId") String listingId,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout);

    /**
     * Find all active, confirmed reservations for a listing where checkout is in the future
     * (for showing booked dates — excludes past bookings).
     */
    @Query("SELECT r FROM Reserve r WHERE r.listing.id = :listingId " +
           "AND r.isCancelled = false " +
           "AND r.status = 'confirmed' " +
           "AND r.checkout > :today")
    List<Reserve> findActiveBookings(
            @Param("listingId") String listingId,
            @Param("today") LocalDate today);

    /**
     * Check if a user already has an active (confirmed, non-cancelled, future) booking for a listing.
     */
    @Query("SELECT r FROM Reserve r WHERE r.listing.id = :listingId " +
           "AND r.reservedBy.id = :userId " +
           "AND r.isCancelled = false " +
           "AND r.status = 'confirmed' " +
           "AND r.checkout > :today")
    List<Reserve> findActiveBookingsByUser(
            @Param("listingId") String listingId,
            @Param("userId") String userId,
            @Param("today") LocalDate today);
}
