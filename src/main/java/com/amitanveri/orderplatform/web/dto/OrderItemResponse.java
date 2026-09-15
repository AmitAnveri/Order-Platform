package com.amitanveri.orderplatform.web.dto;

import com.amitanveri.orderplatform.domain.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(Long productId, int quantity, BigDecimal unitPrice) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProduct().getId(), item.getQuantity(), item.getUnitPrice());
    }
}
