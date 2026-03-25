package com.aimentor.domain.order.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores a user's order and its lifecycle state.
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 30)
    private String paymentMethod;

    @Column(length = 500)
    private String paymentFailureReason;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    @Builder
    public Order(Long userId, BigDecimal totalPrice, OrderStatus status, String address, LocalDateTime orderedAt) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.address = address;
        this.orderedAt = orderedAt;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItem.assignOrder(this);
        this.orderItems.add(orderItem);
    }

    public void addPaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransaction.assignOrder(this);
        this.paymentTransactions.add(paymentTransaction);
    }

    public void preparePayment(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.paymentFailureReason = null;
        this.paidAt = null;
    }

    public void markPaid(String paymentMethod, LocalDateTime paidAt) {
        this.status = OrderStatus.PAID;
        this.paymentMethod = paymentMethod;
        this.paymentFailureReason = null;
        this.paidAt = paidAt;
    }

    public void markPaymentFailed(String paymentMethod, String paymentFailureReason) {
        this.status = OrderStatus.PAYMENT_FAILED;
        this.paymentMethod = paymentMethod;
        this.paymentFailureReason = paymentFailureReason;
        this.paidAt = null;
    }
}
