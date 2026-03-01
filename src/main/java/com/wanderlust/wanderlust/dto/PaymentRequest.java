package com.wanderlust.wanderlust.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank
    private String listingId;

    @NotBlank
    private String checkin;

    @NotBlank
    private String checkout;

    @NotNull @Min(1) @Max(4)
    private Integer adult;

    @Min(0) @Max(2)
    private Integer children = 0;
}
