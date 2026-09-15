package com.amitanveri.orderplatform.web.dto;

import com.amitanveri.orderplatform.domain.Inventory;

public record InventoryResponse(Long productId, int availableQuantity) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getProductId(), inventory.getAvailableQuantity());
    }
}
