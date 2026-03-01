package com.wanderlust.wanderlust.config;

import com.wanderlust.wanderlust.security.JwtAuthenticationFilter;
import com.wanderlust.wanderlust.security.OAuth2AuthenticationSuccessHandler;
import com.wanderlust.wanderlust.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // SOC2: Security headers
                .headers(headers -> headers
                        .contentTypeOptions(opt -> {})                          // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.deny())                    // X-Frame-Options: DENY
                        .httpStrictTransportSecurity(hsts -> hsts              // HSTS
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .cacheControl(cache -> {})                              // Cache-Control: no-cache
                        .xssProtection(xss -> xss.headerValue(                 // X-XSS-Protection: 1; mode=block
                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
                                        .HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data: https:; font-src 'self' https:; "
                                        + "connect-src 'self' https://api.razorpay.com https://checkout.razorpay.com; "
                                        + "form-action 'self' https://accounts.google.com https://www.facebook.com https://appleid.apple.com"))
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(self), payment=(self)"))
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/*/reviews").permitAll()
                        // GDPR endpoints (authenticated)
                        .requestMatchers("/api/gdpr/**").authenticated()
                        // Protected endpoints
                        .requestMatchers(HttpMethod.POST, "/api/listings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/listings/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/listings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/listings/*/reviews/**").authenticated()
                        .requestMatchers("/api/reserves/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // OAuth2 social login
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestRepository(authorizationRequestRepository()))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler())
                )
                // SOC2: Rate limiting before auth to block brute-force
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // SOC2: Restrict allowed headers instead of wildcard
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "Cache-Control"));
        configuration.setExposedHeaders(List.of("Authorization", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth2/**", configuration);
        source.registerCorsConfiguration("/login/oauth2/**", configuration);
        return source;
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    /**
     * Redirects to frontend login page with error message when OAuth2 fails.
     */
    @Bean
    public AuthenticationFailureHandler oAuth2FailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception) -> {
            String errorMessage = exception.getLocalizedMessage() != null
                    ? exception.getLocalizedMessage()
                    : "Social login failed";
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", errorMessage)
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
