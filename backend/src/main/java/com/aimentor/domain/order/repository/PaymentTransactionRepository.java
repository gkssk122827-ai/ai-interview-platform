package com.aimentor.domain.order.repository;

import com.aimentor.domain.order.entity.PaymentTransaction;
import com.aimentor.domain.order.entity.PaymentTransactionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    Optional<PaymentTransaction> findFirstByOrderIdAndStatusOrderByRequestedAtDesc(Long orderId, PaymentTransactionStatus status);
}
