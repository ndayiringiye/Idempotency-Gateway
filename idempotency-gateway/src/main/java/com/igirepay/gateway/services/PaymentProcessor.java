package com.igirepay.gateway.services;

import com.igirepay.gateway.model.PaymentRequest;
import com.igirepay.gateway.model.PaymentResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentProcessor {

    public PaymentResponse processPayment(PaymentRequest request, long delay) throws InterruptedException {
        // Simulate real payment processing delay (User Story 1)
        Thread.sleep(delay);

        String message = "Charged " + request.getAmount() + " " + request.getCurrency();

        return new PaymentResponse(
                request.getAmount(),
                request.getCurrency(),
                message
        );
    }
}