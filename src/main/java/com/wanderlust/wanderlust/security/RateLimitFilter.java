package com.wanderlust.wanderlust.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOC2 Compliance: Rate limiting filter to prevent brute-force and DDoS attacks.
 * 
 * Limits:
 * - Auth endpoints (login/signup): 10 requests per minute per IP
 * - General API: 100 requests per minute per IP
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.startsWith("/api/auth/")) {
            // Strict rate limit for auth endpoints
            bucket = authBuckets.computeIfAbsent(clientIp, this::createAuthBucket);
        } else if (path.startsWith("/api/")) {
            // General API rate limit
            bucket = apiBuckets.computeIfAbsent(clientIp, this::createApiBucket);
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Remaining",
                    String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Please try again later.\","
                            + "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
        }
    }

    private Bucket createAuthBucket(String key) {
        // 10 requests per minute for auth endpoints
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket(String key) {
        // 100 requests per minute for general API
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
