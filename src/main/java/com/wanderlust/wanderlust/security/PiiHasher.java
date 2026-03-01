package com.wanderlust.wanderlust.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SOC2 & GDPR Compliance: Keyed HMAC-SHA256 hashing for blind index lookups.
 * 
 * Used alongside PiiEncryptor to enable database queries on encrypted PII fields.
 * The same plaintext always produces the same hash (deterministic), enabling
 * WHERE-clause lookups without exposing the actual data.
 */
@Component
@Slf4j
public class PiiHasher {

    private static String hashKey;

    @Value("${app.security.pii-encryption-key}")
    public void setHashKey(String key) {
        PiiHasher.hashKey = key;
    }

    /**
     * Compute a deterministic HMAC-SHA256 hash of the input.
     * Returns a URL-safe Base64 string suitable for database storage and indexing.
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(hashKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(plaintext.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            log.error("PII hashing failed", e);
            throw new RuntimeException("Failed to hash PII data", e);
        }
    }
}
