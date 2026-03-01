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
public class ListingsResponse {
    private List<ListingDto> listings;
    private long total;
    private int page;
    private int totalPages;
}
