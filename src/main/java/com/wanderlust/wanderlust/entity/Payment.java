package com.wanderlust.wanderlust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending"; // pending, success, failed, refunded

    private String transactionId;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    private String paymentMethod;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserve_id")
    private Reserve reserve;

    // Temporary booking data stored before payment verification
    private String listingId;
    private String checkin;
    private String checkout;
    private Integer adult;
    private Integer children;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Refund refund;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
