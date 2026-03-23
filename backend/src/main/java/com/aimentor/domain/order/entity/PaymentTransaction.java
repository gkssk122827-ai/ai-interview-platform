package com.aimentor.domain.order.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 30)
    private String paymentMethod;

    @Column(nullable = false, length = 100)
    private String transactionKey;

    @Column(length = 100)
    private String providerTransactionId;

    @Column(length = 100)
    private String partnerOrderId;

    @Column(length = 100)
    private String partnerUserId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Builder
    public PaymentTransaction(
            String provider,
            String paymentMethod,
            String transactionKey,
            String providerTransactionId,
            String partnerOrderId,
            String partnerUserId,
            BigDecimal amount,
            PaymentTransactionStatus status,
            String failureReason,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt
    ) {
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.transactionKey = transactionKey;
        this.providerTransactionId = providerTransactionId;
        this.partnerOrderId = partnerOrderId;
        this.partnerUserId = partnerUserId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
    }

    public void markReady(String providerTransactionId) {
        this.status = PaymentTransactionStatus.READY;
        this.providerTransactionId = providerTransactionId;
        this.failureReason = null;
        this.approvedAt = null;
    }

    public void markSuccess(String providerTransactionId, LocalDateTime approvedAt) {
        this.status = PaymentTransactionStatus.SUCCESS;
        this.providerTransactionId = providerTransactionId;
        this.failureReason = null;
        this.approvedAt = approvedAt;
    }

    public void markFailed(String failureReason) {
        this.status = PaymentTransactionStatus.FAILED;
        this.failureReason = failureReason;
        this.approvedAt = null;
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}
