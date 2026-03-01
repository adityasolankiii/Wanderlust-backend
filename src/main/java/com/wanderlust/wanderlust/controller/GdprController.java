package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.service.GdprService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GDPR Compliance Controller: Endpoints for data subject rights.
 * 
 * - GET  /api/gdpr/export  — Right of Access & Data Portability (Art. 15, 20)
 * - DELETE /api/gdpr/delete — Right to Erasure (Art. 17)
 * - POST /api/gdpr/consent — Record consent (Art. 7)
 */
@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GdprController {

    private final GdprService gdprService;

    /**
     * GDPR Article 15 & 20: Export all personal data for the authenticated user.
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportData(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(gdprService.exportUserData(userDetails.getUsername()));
    }

    /**
     * GDPR Article 17: Delete all personal data (Right to be Forgotten).
     * This action is irreversible.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteData(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(gdprService.deleteUserData(userDetails.getUsername()));
    }

    /**
     * GDPR Article 7: Record user's consent to data processing.
     */
    @PostMapping("/consent")
    public ResponseEntity<Map<String, String>> recordConsent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String policyVersion = body.getOrDefault("policyVersion", "1.0");
        gdprService.recordConsent(userDetails.getUsername(), policyVersion);
        return ResponseEntity.ok(Map.of(
                "message", "Consent recorded successfully",
                "policyVersion", policyVersion
        ));
    }
}
