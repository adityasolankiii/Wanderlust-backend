package com.wanderlust.wanderlust.dto;

import lombok.Data;

@Data
public class PaymentVerificationRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // Booking details for reservation creation on successful payment
    private String mobile;
}
