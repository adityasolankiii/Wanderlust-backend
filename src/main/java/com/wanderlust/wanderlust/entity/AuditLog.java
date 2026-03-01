package com.wanderlust.wanderlust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SOC2 Compliance: Audit trail for all security-relevant actions.
 * Records who did what, when, and from where.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "createdAt"),
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** The user who performed the action (username or "SYSTEM" / "ANONYMOUS") */
    @Column(nullable = false)
    private String username;

    /** Action type: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, VIEW, EXPORT, DATA_DELETION, etc. */
    @Column(nullable = false, length = 50)
    private String action;

    /** Entity type being acted upon: USER, LISTING, RESERVE, PAYMENT, etc. */
    @Column(length = 50)
    private String entityType;

    /** ID of the entity being acted upon */
    @Column(length = 50)
    private String entityId;

    /** Human-readable description of the action */
    @Column(length = 500)
    private String description;

    /** IP address of the requester */
    @Column(length = 45)
    private String ipAddress;

    /** User-Agent header */
    @Column(length = 500)
    private String userAgent;

    /** Whether the action was successful */
    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    /** Failure reason if applicable */
    @Column(length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
