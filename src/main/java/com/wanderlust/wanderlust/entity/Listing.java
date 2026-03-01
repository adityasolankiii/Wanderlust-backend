package com.wanderlust.wanderlust.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank @Size(min = 5, max = 100)
    @Column(nullable = false)
    private String title;

    @NotBlank @Size(min = 20, max = 2000)
    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String imageUrl;

    private String imageFilename;

    @NotNull @Min(100) @Max(1000000)
    @Column(nullable = false)
    private Integer price;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @NotBlank
    @Column(nullable = false)
    private String country;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isReserved = false;

    // Geometry (stored as lat/lng)
    private Double longitude;
    private Double latitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ListingImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Reserve> reserves = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
