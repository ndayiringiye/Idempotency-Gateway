package com.igirepay.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String transactionId;
    private String status;
    private String message;
    private Integer amount;
    private String currency;
    private LocalDateTime timestamp;

    /**
     * Convenience constructor for successful payments
     */
    public PaymentResponse(Integer amount, String currency, String message) {
        this.transactionId = "TX-" + System.currentTimeMillis();
        this.status = "SUCCESS";
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = LocalDateTime.now();
    }
}