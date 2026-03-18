package com.aimentor.domain.order.dto.response;

import com.aimentor.domain.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal totalPrice,
        OrderStatus status,
        String address,
        LocalDateTime orderedAt,
        List<OrderItemResponse> items
) {
}
