package com.wanderlust.wanderlust.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wanderlust.wanderlust.security.PiiEncryptor;
import com.wanderlust.wanderlust.security.PiiHasher;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email_hash"),
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank @Size(min = 3, max = 30)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank @Email
    @Column(nullable = false, length = 512)
    @Convert(converter = PiiEncryptor.class)
    private String email;

    /** Blind index (HMAC-SHA256) for email lookups — enables WHERE queries on encrypted email */
    @Column(name = "email_hash", nullable = false, unique = true, length = 64)
    private String emailHash;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    /** OAuth2 provider (google, facebook, apple) — null for local users */
    @Column(length = 20)
    private String provider;

    /** OAuth2 provider's user ID */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /** Profile picture URL from OAuth2 provider */
    @Column(length = 512)
    private String avatarUrl;

    /** Password reset token (hashed) */
    @JsonIgnore
    @Column(length = 255)
    private String resetToken;

    /** Password reset token expiry */
    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Listing> listings = new ArrayList<>();

    @OneToMany(mappedBy = "reservedBy", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Reserve> reserves = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** GDPR: Timestamp when user consented to data processing */
    private LocalDateTime gdprConsentAt;

    /** GDPR: Version of the privacy policy user consented to */
    @Column(length = 20)
    private String privacyPolicyVersion;
}
