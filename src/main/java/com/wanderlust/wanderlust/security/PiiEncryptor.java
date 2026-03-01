package com.wanderlust.wanderlust.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SOC2 & GDPR Compliance: AES-256-GCM encryption for PII fields stored in the database.
 * 
 * Apply to any entity field containing PII:
 *   @Convert(converter = PiiEncryptor.class)
 *   private String mobile;
 */
@Converter
@Component
@Slf4j
public class PiiEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static String encryptionKey;

    @Value("${app.security.pii-encryption-key}")
    public void setEncryptionKey(String key) {
        PiiEncryptor.encryptionKey = key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) return attribute;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes("UTF-8"));

            // Prepend IV to ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return "ENC:" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("PII encryption failed", e);
            throw new RuntimeException("Failed to encrypt PII data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return dbData;
        if (!dbData.startsWith("ENC:")) return dbData; // backward compat for unencrypted data

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] decoded = Base64.getDecoder().decode(dbData.substring(4));

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return new String(cipher.doFinal(cipherText), "UTF-8");
        } catch (Exception e) {
            log.error("PII decryption failed", e);
            throw new RuntimeException("Failed to decrypt PII data", e);
        }
    }
}
