package com.amitanveri.orderplatform.service;

import com.amitanveri.orderplatform.domain.Inventory;
import com.amitanveri.orderplatform.domain.Order;
import com.amitanveri.orderplatform.domain.OrderItem;
import com.amitanveri.orderplatform.domain.OrderStatus;
import com.amitanveri.orderplatform.domain.Payment;
import com.amitanveri.orderplatform.domain.PaymentStatus;
import com.amitanveri.orderplatform.domain.Product;
import com.amitanveri.orderplatform.repository.InventoryRepository;
import com.amitanveri.orderplatform.repository.OrderRepository;
import com.amitanveri.orderplatform.repository.PaymentRepository;
import com.amitanveri.orderplatform.repository.ProductRepository;
import com.amitanveri.orderplatform.web.dto.CreateOrderRequest;
import com.amitanveri.orderplatform.web.dto.OrderResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Returns DTOs rather than entities, so nothing lazy can be touched after the transaction closes. */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(OrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Reserves stock for every line and records a (simulated) successful payment. The whole thing runs in
     * one transaction, so a failure anywhere leaves inventory untouched.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = aggregateQuantity(request);

        Order order = new Order(request.userId(), OrderStatus.CONFIRMED);
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            Long productId = line.getKey();
            int quantity = line.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
            Inventory inventory = inventoryRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new InsufficientStockException(
                            "No inventory recorded for product " + productId));
            if (inventory.getAvailableQuantity() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product " + productId
                        + " (" + product.getName() + "): requested " + quantity
                        + ", available " + inventory.getAvailableQuantity());
            }

            inventory.reserve(quantity);
            order.addItem(new OrderItem(product, quantity, product.getPrice()));
        }

        Order saved = orderRepository.save(order);
        Payment payment = paymentRepository.save(new Payment(saved, PaymentStatus.SUCCESS));
        return OrderResponse.from(saved, payment);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        Payment payment = paymentRepository.findByOrder_Id(orderId).orElse(null);
        return OrderResponse.from(order, payment);
    }

    private Map<Long, Integer> aggregateQuantity(CreateOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }
}
