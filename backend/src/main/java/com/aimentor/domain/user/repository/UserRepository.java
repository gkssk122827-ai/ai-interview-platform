package com.aimentor.domain.user.repository;

import com.aimentor.domain.user.entity.User;
import com.aimentor.domain.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(String nameKeyword, String emailKeyword);

    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);

    List<User> findAllByOrderByCreatedAtDesc();
}
