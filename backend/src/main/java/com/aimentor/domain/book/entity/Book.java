package com.aimentor.domain.book.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import com.aimentor.domain.cart.entity.CartItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores book information exposed to users and managed by admins.
 */
@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, length = 100)
    private String publisher;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 500)
    private String coverUrl;

    @Column(length = 5000)
    private String description;

    @OneToMany(mappedBy = "book")
    private final List<CartItem> cartItems = new ArrayList<>();

    @Builder
    public Book(
            String title,
            String author,
            String publisher,
            BigDecimal price,
            Integer stock,
            String coverUrl,
            String description
    ) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.price = price;
        this.stock = stock;
        this.coverUrl = coverUrl;
        this.description = description;
    }

    public void update(
            String title,
            String author,
            String publisher,
            BigDecimal price,
            Integer stock,
            String coverUrl,
            String description
    ) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.price = price;
        this.stock = stock;
        this.coverUrl = coverUrl;
        this.description = description;
    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }
}
