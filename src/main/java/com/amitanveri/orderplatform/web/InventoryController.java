package com.amitanveri.orderplatform.web;

import com.amitanveri.orderplatform.service.InventoryService;
import com.amitanveri.orderplatform.web.dto.InventoryResponse;
import com.amitanveri.orderplatform.web.dto.UpsertInventoryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryResponse upsert(@Valid @RequestBody UpsertInventoryRequest request) {
        return inventoryService.upsert(request.productId(), request.availableQuantity());
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return inventoryService.get(productId);
    }
}
