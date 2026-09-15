package com.amitanveri.orderplatform.repository;

import com.amitanveri.orderplatform.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
