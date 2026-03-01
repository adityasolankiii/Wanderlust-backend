package com.wanderlust.wanderlust.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank
    @Size(min = 10, max = 500)
    private String comment;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
}
