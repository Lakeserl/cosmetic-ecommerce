package com.cosmetics.server.repository;

import com.cosmetics.server.entity.ENUM.AuthProvider;
import com.cosmetics.server.entity.ENUM.STATUS;
import com.cosmetics.server.entity.auth.Users;
import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByPhoneNumber(String phoneNumber);
    @Query("SELECT u from Users u WHERE u.email = :username OR u.phoneNumber = :username")
    Optional<Users> findByUsername(@Param("username") String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Users> findByProviderAndProviderId(@Param("provider") AuthProvider provider,
                                                @Param("providerId") String providerId);

    @Query("SELECT u from Users u where u.status = :status")
    Optional<Users> findByAllByStatus(@Param("status") STATUS status);

    @Query("SELECT u FROM Users u WHERE u.createdAt >= :fromDate")
    Optional<Users> findRecentUsers(@Param("fromDate")LocalDateTime fromDate);

    @Modifying
    @Query("UPDATE Users u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") STATUS status);

    @Modifying
    @Query("UPDATE Users u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
