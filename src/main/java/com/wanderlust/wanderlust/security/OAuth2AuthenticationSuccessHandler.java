package com.wanderlust.wanderlust.security;

import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.repository.UserRepository;
import com.wanderlust.wanderlust.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles successful OAuth2 authentication.
 * Creates or updates the local user record, generates a JWT,
 * and redirects to the frontend with the token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();
        String provider = authToken.getAuthorizedClientRegistrationId(); // google, facebook, apple

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub"); // Google, Apple use "sub"
        String picture = extractPictureUrl(oAuth2User, provider);

        // Facebook uses "id" instead of "sub"
        if (providerId == null) {
            providerId = oAuth2User.getAttribute("id");
        }

        if (email == null) {
            log.warn("OAuth2 login without email from provider: {}", provider);
            String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", "Email is required for social login")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        String emailHash = PiiHasher.hash(email);

        // Find existing user by provider+providerId, or by email hash
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.findByEmailHash(emailHash).orElse(null));

        if (user == null) {
            // New social user — create account
            String username = generateUsername(name, email);

            user = User.builder()
                    .username(username)
                    .email(email)
                    .emailHash(emailHash)
                    .provider(provider)
                    .providerId(providerId)
                    .avatarUrl(picture)
                    .gdprConsentAt(LocalDateTime.now())
                    .privacyPolicyVersion("1.0")
                    .build();

            user = userRepository.save(user);
            auditService.logAction(username, "OAUTH2_SIGNUP", "USER",
                    user.getId(), "Social signup via " + provider, request);
            log.info("New OAuth2 user created: {} via {}", username, provider);
        } else {
            // Existing user — update/link provider info
            if (user.getProvider() == null) {
                // Local user linking social account for first time
                user.setProvider(provider);
                user.setProviderId(providerId);
            } else if (!user.getProvider().equals(provider)) {
                // User has different provider — update to current one for multi-provider linking
                user.setProvider(provider);
                user.setProviderId(providerId);
            }
            if (picture != null) {
                user.setAvatarUrl(picture);
            }
            userRepository.save(user);
            auditService.logAction(user.getUsername(), "OAUTH2_LOGIN", "USER",
                    user.getId(), "Social login via " + provider, request);
        }

        // Generate JWT
        String jwt = jwtTokenProvider.generateToken(user.getUsername());

        // Redirect to frontend with token only (no email in URL for privacy)
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", jwt)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Extract profile picture URL, handling Facebook's nested structure.
     */
    @SuppressWarnings("unchecked")
    private String extractPictureUrl(OAuth2User oAuth2User, String provider) {
        if ("facebook".equals(provider)) {
            Object pictureObj = oAuth2User.getAttribute("picture");
            if (pictureObj instanceof Map) {
                Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
                Object dataObj = pictureMap.get("data");
                if (dataObj instanceof Map) {
                    return (String) ((Map<String, Object>) dataObj).get("url");
                }
            }
            return null;
        }
        return oAuth2User.getAttribute("picture");
    }

    private String generateUsername(String name, String email) {
        String base;
        if (name != null && !name.isBlank()) {
            base = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        } else {
            base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }

        if (base.length() < 3) base = base + "user";
        if (base.length() > 20) base = base.substring(0, 20);

        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter;
            counter++;
        }

        return username;
    }
}
