package com.aimentor.domain.cart.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.cart.dto.request.CartItemCreateRequest;
import com.aimentor.domain.cart.dto.request.CartItemUpdateRequest;
import com.aimentor.domain.cart.dto.response.CartItemResponse;
import com.aimentor.domain.cart.dto.response.CartResponse;
import com.aimentor.domain.cart.entity.CartItem;
import com.aimentor.domain.cart.repository.CartItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages cart items for the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public CartItemService(CartItemRepository cartItemRepository, BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    public CartResponse getCart(Long userId) {
        List<CartItemResponse> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::linePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(items, totalQuantity, totalPrice);
    }

    @Transactional
    public CartItemResponse addCartItem(Long userId, CartItemCreateRequest request) {
        Book book = getBook(request.bookId());
        CartItem cartItem = cartItemRepository.findByUserIdAndBookId(userId, request.bookId())
                .orElseGet(() -> cartItemRepository.save(CartItem.builder()
                        .userId(userId)
                        .bookId(book.getId())
                        .quantity(0)
                        .build()));

        int nextQuantity = cartItem.getQuantity() + request.quantity();
        validateStock(book, nextQuantity);
        cartItem.addQuantity(request.quantity());
        return toResponse(cartItem, book);
    }

    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long bookId, CartItemUpdateRequest request) {
        CartItem cartItem = getCartItem(userId, bookId);
        Book book = getBook(bookId);
        validateStock(book, request.quantity());
        cartItem.updateQuantity(request.quantity());
        return toResponse(cartItem, book);
    }

    @Transactional
    public void deleteCartItem(Long userId, Long bookId) {
        cartItemRepository.delete(getCartItem(userId, bookId));
    }

    private CartItem getCartItem(Long userId, Long bookId) {
        return cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "CART_ITEM_NOT_FOUND",
                        "Cart item not found."
                ));
    }

    private Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found."));
    }

    private void validateStock(Book book, int quantity) {
        if (quantity > book.getStock()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CART_QUANTITY_EXCEEDS_STOCK",
                    "Requested quantity exceeds available stock."
            );
        }
    }

    private CartItemResponse toResponse(CartItem cartItem) {
        return toResponse(cartItem, cartItem.getBook());
    }

    private CartItemResponse toResponse(CartItem cartItem, Book book) {
        BigDecimal linePrice = book.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getUserId(),
                cartItem.getBookId(),
                cartItem.getQuantity(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPrice(),
                book.getCoverUrl(),
                linePrice,
                cartItem.getCreatedAt(),
                cartItem.getUpdatedAt()
        );
    }
}
