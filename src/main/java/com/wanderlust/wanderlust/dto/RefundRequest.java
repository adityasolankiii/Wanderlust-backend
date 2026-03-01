package com.wanderlust.wanderlust.dto;

import lombok.Data;

@Data
public class RefundRequest {
    private String paymentId;
    private Integer amount;
    private String reason;
}
