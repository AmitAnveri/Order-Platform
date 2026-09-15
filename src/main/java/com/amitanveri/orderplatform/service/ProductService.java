package com.amitanveri.orderplatform.service;

import com.amitanveri.orderplatform.domain.Product;
import com.amitanveri.orderplatform.repository.ProductRepository;
import com.amitanveri.orderplatform.web.dto.ProductResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Returns DTOs rather than entities, so nothing lazy can be touched after the transaction closes. */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(String name, BigDecimal price) {
        return ProductResponse.from(productRepository.save(new Product(name, price)));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        return ProductResponse.from(product);
    }
}
