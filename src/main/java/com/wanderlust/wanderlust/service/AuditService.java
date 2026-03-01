package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.entity.AuditLog;
import com.wanderlust.wanderlust.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SOC2 Compliance: Centralized audit logging service.
 * All security-relevant actions are recorded asynchronously
 * to avoid impacting request performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log a successful action.
     */
    @Async
    public void logAction(String username, String action, String entityType,
                          String entityId, String description, HttpServletRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username != null ? username : "ANONYMOUS")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 500) : null)
                    .success(true)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit: {} by {} on {}:{}", action, username, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Log a failed action (e.g., failed login, unauthorized access).
     */
    @Async
    public void logFailure(String username, String action, String entityType,
                           String entityId, String reason, HttpServletRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username != null ? username : "ANONYMOUS")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description("FAILED: " + reason)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 500) : null)
                    .success(false)
                    .failureReason(reason)
                    .build();

            auditLogRepository.save(auditLog);
            log.warn("Audit FAILURE: {} by {} - {}", action, username, reason);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Simple overload without HttpServletRequest for backend-only operations.
     */
    @Async
    public void logAction(String username, String action, String entityType,
                          String entityId, String description) {
        logAction(username, action, entityType, entityId, description, null);
    }

    /**
     * Extract client IP, accounting for proxies.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) return "INTERNAL";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
