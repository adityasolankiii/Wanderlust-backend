package com.wanderlust.wanderlust.entity;

import com.wanderlust.wanderlust.security.PiiEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserves")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reserve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkin;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkout;

    @NotNull @Min(1) @Max(4)
    @Column(nullable = false)
    private Integer adult;

    @NotNull @Min(0) @Max(2)
    @Column(nullable = false)
    @Builder.Default
    private Integer children = 0;

    @NotBlank
    @Column(nullable = false, length = 512)
    @Convert(converter = PiiEncryptor.class)
    private String mobile;

    @NotNull
    @Column(nullable = false)
    private Integer total;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending"; // pending, confirmed, cancelled

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCancelled = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reserved_by_id", nullable = false)
    private User reservedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @OneToOne(mappedBy = "reserve", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
