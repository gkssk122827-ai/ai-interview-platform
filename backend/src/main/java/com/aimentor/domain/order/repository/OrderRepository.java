package com.aimentor.domain.order.repository;

import com.aimentor.domain.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Loads orders with their items for the authenticated user.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book"})
    List<Order> findByUserIdOrderByOrderedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
