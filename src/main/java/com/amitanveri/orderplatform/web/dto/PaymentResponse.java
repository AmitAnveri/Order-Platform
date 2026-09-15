package com.amitanveri.orderplatform.web.dto;

import com.amitanveri.orderplatform.domain.Payment;
import com.amitanveri.orderplatform.domain.PaymentStatus;

public record PaymentResponse(Long id, PaymentStatus status) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getStatus());
    }
}
