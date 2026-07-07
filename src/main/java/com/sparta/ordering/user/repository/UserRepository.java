package com.sparta.ordering.user.repository;

import com.sparta.ordering.user.entity.User;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUserNameAndDeletedAtIsNull(String userName);
    Optional<User> findByIdAndDeletedAtIsNull(UUID userId);
}
