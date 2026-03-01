package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.repository.UserRepository;
import com.wanderlust.wanderlust.security.JwtTokenProvider;
import com.wanderlust.wanderlust.security.PiiHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.password-reset.token-expiry-minutes:30}")
    private int resetTokenExpiryMinutes;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    public AuthResponse signup(SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailHash(PiiHasher.hash(request.getEmail()))) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .emailHash(PiiHasher.hash(request.getEmail()))
                .password(passwordEncoder.encode(request.getPassword()))
                .gdprConsentAt(LocalDateTime.now())
                .privacyPolicyVersion("1.0")
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername());

        return AuthResponse.builder()
                .user(UserDto.from(user))
                .token(token)
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Check if this is an OAuth2-only user (no password set)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPassword() == null && user.getProvider() != null) {
            throw new IllegalArgumentException(
                    "This account uses " + user.getProvider() + " sign-in. Please use the \"" +
                    user.getProvider().substring(0, 1).toUpperCase() + user.getProvider().substring(1) +
                    "\" button to log in.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication.getName());

        return AuthResponse.builder()
                .user(UserDto.from(user))
                .token(token)
                .message("Login successful")
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Forgot Password: Generate reset token and send email with reset link.
     * Always returns success message to prevent email enumeration attacks.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String emailHash = PiiHasher.hash(request.getEmail());
        userRepository.findByEmailHash(emailHash).ifPresent(user -> {
            // Don't allow password reset for OAuth2-only users
            if (user.getProvider() != null && user.getPassword() == null) {
                log.info("Password reset requested for OAuth2 user: {}", user.getUsername());
                return;
            }

            String token = UUID.randomUUID().toString();
            user.setResetToken(hashToken(token));
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes));
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
            log.info("Password reset email sent for user: {}", user.getUsername());
        });
    }

    /**
     * Reset Password: Validate token and update password.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByResetToken(hashToken(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // Clear expired token
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    /**
     * Hash a token with SHA-256 for secure storage.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
