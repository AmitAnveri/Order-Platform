package com.amitanveri.orderplatform.service;

import com.amitanveri.orderplatform.domain.Inventory;
import com.amitanveri.orderplatform.domain.Product;
import com.amitanveri.orderplatform.repository.InventoryRepository;
import com.amitanveri.orderplatform.repository.ProductRepository;
import com.amitanveri.orderplatform.web.dto.InventoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Returns DTOs rather than entities, so nothing lazy can be touched after the transaction closes. */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates the inventory row for a product, or replaces the quantity if one already exists.
     */
    @Transactional
    public InventoryResponse upsert(Long productId, int availableQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        Inventory inventory = inventoryRepository.findById(productId)
                .map(existing -> {
                    existing.setAvailableQuantity(availableQuantity);
                    return existing;
                })
                .orElseGet(() -> new Inventory(product, availableQuantity));
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Inventory for product " + productId + " not found"));
        return InventoryResponse.from(inventory);
    }
}
