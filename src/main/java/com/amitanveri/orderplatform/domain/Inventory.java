package com.amitanveri.orderplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    private Long productId;

    /** Shares its primary key with the product, so there is exactly one inventory row per product. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private Long version;

    protected Inventory() {
    }

    public Inventory(Product product, int availableQuantity) {
        this.product = product;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Product getProduct() {
        return product;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void reserve(int quantity) {
        this.availableQuantity -= quantity;
    }
}
