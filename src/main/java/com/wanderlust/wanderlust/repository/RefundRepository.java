package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    Optional<Refund> findByPaymentId(String paymentId);
}
