package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.createOrder(request, userDetails.getUsername()));
    }

    @PostMapping("/verify")
    public ResponseEntity<ReserveDto> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.verifyPayment(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/reserve/{reserveId}")
    public ResponseEntity<PaymentDto> getByReserve(@PathVariable String reserveId) {
        return ResponseEntity.ok(paymentService.getByReserve(reserveId));
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> processRefund(@RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.processRefund(request));
    }
}
