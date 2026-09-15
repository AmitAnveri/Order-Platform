package com.amitanveri.orderplatform.web.dto;

import com.amitanveri.orderplatform.domain.Order;
import com.amitanveri.orderplatform.domain.OrderStatus;
import com.amitanveri.orderplatform.domain.Payment;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        String userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        PaymentResponse payment) {

    public static OrderResponse from(Order order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                payment == null ? null : PaymentResponse.from(payment));
    }
}
