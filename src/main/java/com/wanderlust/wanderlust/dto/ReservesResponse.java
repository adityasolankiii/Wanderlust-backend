package com.wanderlust.wanderlust.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservesResponse {
    private List<ReserveDto> reserves;
    private long total;
    private int page;
    private int totalPages;
}
