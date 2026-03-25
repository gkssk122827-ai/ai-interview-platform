package com.aimentor.domain.user.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores application users and their refresh token state.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(length = 1000)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiresAt;

    @Builder
    public User(String email, String name, String phone, String password, Role role) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
        if (status == UserStatus.SUSPENDED || status == UserStatus.WITHDRAWN) {
            clearRefreshToken();
        }
    }
}
