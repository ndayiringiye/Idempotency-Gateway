package com.igirepay.gateway.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Data
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Integer amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    /**
     * Innovation: Generates an immutable cryptographic hash of the transaction data.
     * This helps in detecting if the same Idempotency-Key is used with different payloads.
     */
    public String generatePayloadHash() {
        try {
            // More robust format to avoid collisions
            String rawString = amount + ":" + currency.trim().toUpperCase();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');   // Fixed: was appending to wrong variable
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cryptographic hashing failed", e);
        }
    }
}