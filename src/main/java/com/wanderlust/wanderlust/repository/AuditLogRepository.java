package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOC2 Compliance: Repository for audit log persistence and querying.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLog> findByUsernameAndCreatedAtBetween(
            String username, LocalDateTime start, LocalDateTime end);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId, Pageable pageable);

    long countByUsernameAndActionAndCreatedAtAfter(
            String username, String action, LocalDateTime since);
}
