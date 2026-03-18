package com.aimentor.domain.cart.repository;

import com.aimentor.domain.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Loads cart items for a specific user and book pair.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);
}
