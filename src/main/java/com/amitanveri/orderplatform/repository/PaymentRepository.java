package com.amitanveri.orderplatform.repository;

import com.amitanveri.orderplatform.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Underscore forces the property path onto the association's id; plain findByOrderId no longer resolves. */
    Optional<Payment> findByOrder_Id(Long orderId);
}
