package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.Listing;
import com.wanderlust.wanderlust.entity.Payment;
import com.wanderlust.wanderlust.entity.Refund;
import com.wanderlust.wanderlust.entity.Reserve;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.exception.ResourceNotFoundException;
import com.wanderlust.wanderlust.exception.UnauthorizedAccessException;
import com.wanderlust.wanderlust.repository.ListingRepository;
import com.wanderlust.wanderlust.repository.PaymentRepository;
import com.wanderlust.wanderlust.repository.RefundRepository;
import com.wanderlust.wanderlust.repository.ReserveRepository;
import com.wanderlust.wanderlust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EmailService emailService;

    public ReserveDto getById(String id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        return ReserveDto.from(reserve);
    }

    public ReservesResponse getByUser(String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Reserve> reservesPage = reserveRepository.findByReservedByIdOrderByCreatedAtDesc(userId, pageable);

        return ReservesResponse.builder()
                .reserves(reservesPage.getContent().stream().map(ReserveDto::from).toList())
                .total(reservesPage.getTotalElements())
                .page(page)
                .totalPages(reservesPage.getTotalPages())
                .build();
    }

    public ReservesResponse getMyReservations(String username, int page, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return getByUser(user.getId(), page, limit);
    }

    public ReservesResponse getByListing(String listingId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Reserve> reservesPage = reserveRepository.findByListingIdOrderByCreatedAtDesc(listingId, pageable);

        return ReservesResponse.builder()
                .reserves(reservesPage.getContent().stream().map(ReserveDto::from).toList())
                .total(reservesPage.getTotalElements())
                .page(page)
                .totalPages(reservesPage.getTotalPages())
                .build();
    }

    @Transactional
    public ReserveDto create(ReserveRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        // Check date-based availability
        LocalDate checkin = LocalDate.parse(request.getCheckin());
        LocalDate checkout = LocalDate.parse(request.getCheckout());

        // Validate dates
        if (checkin.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }

        if (!checkout.isAfter(checkin)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        List<Reserve> overlapping = reserveRepository.findOverlappingReservations(
                request.getListingId(), checkin, checkout);
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "This property is already booked for the selected dates. Please choose different dates.");
        }

        long nights = ChronoUnit.DAYS.between(checkin, checkout);
        int total = (int) (listing.getPrice() * nights);

        Reserve reserve = Reserve.builder()
                .checkin(checkin)
                .checkout(checkout)
                .adult(request.getAdult())
                .children(request.getChildren() != null ? request.getChildren() : 0)
                .mobile(request.getMobile())
                .total(total)
                .status("pending") // Booking pending until payment is confirmed
                .reservedBy(user)
                .listing(listing)
                .build();

        reserve = reserveRepository.save(reserve);

        // Note: Listing will be marked as reserved only after successful payment

        return ReserveDto.from(reserve);
    }

    @Transactional
    public ReserveDto cancel(String id, String username, String reason) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reserve.getReservedBy().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this reservation");
        }

        if (reserve.getIsCancelled()) {
            throw new IllegalArgumentException("This booking is already cancelled");
        }

        reserve.setIsCancelled(true);
        reserve.setStatus("cancelled");

        // Process refund if payment was successful
        Payment payment = reserve.getPayment();
        String refundId = null;
        if (payment != null && "success".equals(payment.getStatus())) {
            Refund refund = Refund.builder()
                    .amount(payment.getAmount())
                    .reason(reason != null ? reason : "Cancelled by user")
                    .status("processed")
                    .razorpayRefundId("rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14))
                    .payment(payment)
                    .build();
            refund = refundRepository.save(refund);
            refundId = refund.getRazorpayRefundId();

            payment.setStatus("refunded");
            paymentRepository.save(payment);
        }

        reserve = reserveRepository.save(reserve);

        // Send cancellation email
        emailService.sendCancellationEmail(reserve, reason, refundId);

        return ReserveDto.from(reserve);
    }

    @Transactional
    public void delete(String id, String username) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reserve.getReservedBy().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this reservation");
        }

        reserveRepository.delete(reserve);
    }

    public Map<String, Object> calculatePrice(String listingId, String checkin, String checkout,
                                                int adult, int children) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        LocalDate checkinDate = LocalDate.parse(checkin);
        LocalDate checkoutDate = LocalDate.parse(checkout);
        long nights = ChronoUnit.DAYS.between(checkinDate, checkoutDate);

        int pricePerNight = listing.getPrice();
        int total = (int) (pricePerNight * nights);

        return Map.of(
                "total", total,
                "nights", nights,
                "pricePerNight", pricePerNight
        );
    }

    /**
     * Returns all confirmed, future/current booked date ranges for a listing.
     * Past bookings (checkout <= today) are excluded.
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getBookedDates(String listingId) {
        // Verify listing exists
        listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        List<Reserve> activeReserves = reserveRepository.findActiveBookings(listingId, LocalDate.now());

        return activeReserves.stream()
                .map(r -> Map.of(
                        "checkin", r.getCheckin().toString(),
                        "checkout", r.getCheckout().toString()
                ))
                .toList();
    }
}
