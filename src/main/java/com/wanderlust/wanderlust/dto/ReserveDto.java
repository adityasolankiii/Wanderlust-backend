package com.wanderlust.wanderlust.dto;

import com.wanderlust.wanderlust.entity.Reserve;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveDto {
    private String id;
    private LocalDate checkin;
    private LocalDate checkout;
    private Integer adult;
    private Integer children;
    private String mobile;
    private Integer total;
    private String status;
    private Boolean isCancelled;
    private UserDto reservedBy;
    private ListingDto listing;
    private PaymentDto payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReserveDto from(Reserve reserve) {
        return ReserveDto.builder()
                .id(reserve.getId())
                .checkin(reserve.getCheckin())
                .checkout(reserve.getCheckout())
                .adult(reserve.getAdult())
                .children(reserve.getChildren())
                .mobile(reserve.getMobile())
                .total(reserve.getTotal())
                .status(reserve.getStatus())
                .isCancelled(reserve.getIsCancelled())
                .reservedBy(UserDto.from(reserve.getReservedBy()))
                .listing(ListingDto.from(reserve.getListing()))
                .payment(reserve.getPayment() != null ? PaymentDto.from(reserve.getPayment()) : null)
                .createdAt(reserve.getCreatedAt())
                .updatedAt(reserve.getUpdatedAt())
                .build();
    }
}
