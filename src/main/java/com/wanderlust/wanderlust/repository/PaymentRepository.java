package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByReserveId(String reserveId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
