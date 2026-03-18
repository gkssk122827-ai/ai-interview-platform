package com.aimentor.domain.order.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.cart.entity.CartItem;
import com.aimentor.domain.cart.repository.CartItemRepository;
import com.aimentor.domain.order.dto.request.OrderCreateRequest;
import com.aimentor.domain.order.dto.response.OrderItemResponse;
import com.aimentor.domain.order.dto.response.OrderResponse;
import com.aimentor.domain.order.entity.Order;
import com.aimentor.domain.order.entity.OrderItem;
import com.aimentor.domain.order.entity.OrderStatus;
import com.aimentor.domain.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages order creation and lookup for the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            BookRepository bookRepository
    ) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_EMPTY", "Cart is empty.");
        }

        BigDecimal totalPrice = calculateTotalPrice(cartItems);

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .address(request.address())
                .orderedAt(LocalDateTime.now())
                .build();

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            order.addOrderItem(OrderItem.builder()
                    .bookId(book.getId())
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .build());
        }

        Order savedOrder = orderRepository.save(order);
        List<OrderItemResponse> createdItems = buildCreatedItemResponses(savedOrder, cartItems);
        cartItemRepository.deleteAllInBatch(cartItems);
        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getAddress(),
                savedOrder.getOrderedAt(),
                createdItems
        );
    }

    @Transactional
    public OrderResponse payOrder(Long userId, Long orderId) {
        Order order = getOrderEntity(userId, orderId);
        validatePayable(order);

        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = getBook(orderItem.getBookId());
            if (book.getStock() < orderItem.getQuantity()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", "Book stock is insufficient.");
            }
        }

        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = getBook(orderItem.getBookId());
            book.decreaseStock(orderItem.getQuantity());
        }

        order.markPaid();
        return toResponse(order);
    }

    public List<OrderResponse> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        return toResponse(getOrderEntity(userId, orderId));
    }

    private Order getOrderEntity(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));
    }

    private void validatePayable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_NOT_PAYABLE", "Order cannot be paid in its current status.");
        }
    }

    private Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found."));
    }

    private BigDecimal calculateTotalPrice(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(cartItem -> cartItem.getBook().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItemResponse> buildCreatedItemResponses(Order order, List<CartItem> cartItems) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (int i = 0; i < order.getOrderItems().size(); i++) {
            OrderItem orderItem = order.getOrderItems().get(i);
            Book book = cartItems.get(i).getBook();
            items.add(new OrderItemResponse(
                    orderItem.getId(),
                    order.getId(),
                    orderItem.getBookId(),
                    orderItem.getQuantity(),
                    orderItem.getPrice(),
                    book.getTitle(),
                    book.getCoverUrl()
            ));
        }
        return items;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemResponse(
                        orderItem.getId(),
                        order.getId(),
                        orderItem.getBookId(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getBook().getTitle(),
                        orderItem.getBook().getCoverUrl()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getAddress(),
                order.getOrderedAt(),
                items
        );
    }
}
