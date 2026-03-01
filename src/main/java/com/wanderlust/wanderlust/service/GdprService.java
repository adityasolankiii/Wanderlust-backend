package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.entity.*;
import com.wanderlust.wanderlust.exception.ResourceNotFoundException;
import com.wanderlust.wanderlust.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GDPR Compliance: Handles data subject rights — export, erasure, and consent.
 * 
 * Supports:
 * - Right of Access (Article 15): Export all personal data
 * - Right to Erasure (Article 17): Delete/anonymize personal data
 * - Right to Data Portability (Article 20): Machine-readable export
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GdprService {

    private final UserRepository userRepository;
    private final ReserveRepository reserveRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    /**
     * GDPR Article 15 & 20: Export all personal data for a user in a portable JSON-ready format.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportDate", java.time.LocalDateTime.now().toString());
        export.put("dataController", "Wanderlust Property Booking Platform");
        export.put("dataSubject", exportUserProfile(user));
        export.put("reservations", exportUserReservations(user));
        export.put("reviews", exportUserReviews(user));
        export.put("activityLog", exportUserAuditLog(user));

        auditService.logAction(username, "DATA_EXPORT", "USER", user.getId(),
                "User exported personal data (GDPR Article 15/20)");

        return export;
    }

    /**
     * GDPR Article 17: Right to Erasure — anonymize and delete user data.
     * Note: Financial records required by law are retained with anonymized identifiers.
     */
    @Transactional
    public Map<String, Object> deleteUserData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String userId = user.getId();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Anonymize reviews (keep content for platform integrity, remove identity)
        List<Review> reviews = reviewRepository.findByAuthorId(userId);
        for (Review review : reviews) {
            review.setAuthor(null);
        }
        reviewRepository.saveAll(reviews);
        result.put("reviewsAnonymized", reviews.size());

        // 2. Anonymize reservations (keep for financial/legal records)
        List<Reserve> reserves = reserveRepository.findByReservedById(userId);
        for (Reserve reserve : reserves) {
            reserve.setMobile("DELETED");
        }
        reserveRepository.saveAll(reserves);
        result.put("reservationsAnonymized", reserves.size());

        // 3. Log deletion BEFORE deleting the user
        auditService.logAction(username, "DATA_DELETION", "USER", userId,
                "User data erased (GDPR Article 17). Reviews: " + reviews.size()
                        + ", Reservations: " + reserves.size());

        // 4. Delete user account (cascades to listings, remaining linked data)
        userRepository.delete(user);
        result.put("accountDeleted", true);

        result.put("message", "Your personal data has been erased in accordance with GDPR Article 17. "
                + "Anonymized financial records are retained as required by law.");

        return result;
    }

    /**
     * GDPR Article 7: Record user's consent to data processing.
     */
    @Transactional
    public void recordConsent(String username, String policyVersion) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setGdprConsentAt(java.time.LocalDateTime.now());
        user.setPrivacyPolicyVersion(policyVersion);
        userRepository.save(user);

        auditService.logAction(username, "CONSENT_GIVEN", "USER", user.getId(),
                "User consented to privacy policy v" + policyVersion);
    }

    // ─── Private Helpers ───

    private Map<String, Object> exportUserProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("accountCreated", formatDateTime(user.getCreatedAt()));
        profile.put("lastUpdated", formatDateTime(user.getUpdatedAt()));
        profile.put("gdprConsentGiven", user.getGdprConsentAt() != null);
        profile.put("gdprConsentDate", user.getGdprConsentAt() != null
                ? formatDateTime(user.getGdprConsentAt()) : null);
        return profile;
    }

    private List<Map<String, Object>> exportUserReservations(User user) {
        return reserveRepository.findByReservedById(user.getId()).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("listingTitle", r.getListing() != null ? r.getListing().getTitle() : "N/A");
                    m.put("checkin", r.getCheckin().toString());
                    m.put("checkout", r.getCheckout().toString());
                    m.put("adults", r.getAdult());
                    m.put("children", r.getChildren());
                    m.put("total", r.getTotal());
                    m.put("status", r.getStatus());
                    m.put("isCancelled", r.getIsCancelled());
                    m.put("createdAt", formatDateTime(r.getCreatedAt()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserReviews(User user) {
        return reviewRepository.findByAuthorId(user.getId()).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("listingTitle", r.getListing() != null ? r.getListing().getTitle() : "N/A");
                    m.put("rating", r.getRating());
                    m.put("comment", r.getComment());
                    m.put("createdAt", formatDateTime(r.getCreatedAt()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserAuditLog(User user) {
        return auditLogRepository.findByUsernameAndCreatedAtBetween(
                        user.getUsername(),
                        user.getCreatedAt(),
                        java.time.LocalDateTime.now())
                .stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("action", a.getAction());
                    m.put("entityType", a.getEntityType());
                    m.put("description", a.getDescription());
                    m.put("timestamp", formatDateTime(a.getCreatedAt()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
