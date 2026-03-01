package com.wanderlust.wanderlust.dto;

import com.wanderlust.wanderlust.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private String id;
    private String status;
    private String transactionId;
    private Integer amount;
    private String currency;
    private String paymentMethod;
    private String reserveId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentDto from(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .reserveId(payment.getReserve() != null ? payment.getReserve().getId() : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
