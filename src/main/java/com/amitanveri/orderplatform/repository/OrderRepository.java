package com.amitanveri.orderplatform.repository;

import com.amitanveri.orderplatform.domain.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Fetches items and their products up front so nothing is left lazy once the transaction ends. */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(Long id);
}
