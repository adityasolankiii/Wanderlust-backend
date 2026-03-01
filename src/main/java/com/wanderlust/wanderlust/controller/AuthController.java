package com.wanderlust.wanderlust.controller;

import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.service.AuditService;
import com.wanderlust.wanderlust.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.signup(request);
        auditService.logAction(request.getUsername(), "SIGNUP", "USER",
                response.getUser().getId(), "New user registered", httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request);
            auditService.logAction(request.getUsername(), "LOGIN", "USER",
                    response.getUser().getId(), "User logged in", httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditService.logFailure(request.getUsername(), "LOGIN_FAILED", "USER",
                    null, e.getMessage(), httpRequest);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        if (userDetails != null) {
            auditService.logAction(userDetails.getUsername(), "LOGOUT", "USER",
                    null, "User logged out", httpRequest);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of("message",
                "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. You can now login."));
    }
}
