package com.amitanveri.orderplatform.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpsertInventoryRequest(
        @NotNull Long productId,
        @NotNull @PositiveOrZero Integer availableQuantity) {
}
