package com.cosmetics.server.repository;

import com.cosmetics.server.entity.auth.Token;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByRefreshTokenHashAndRevokedFalse(String refreshTokenHash);
    List<Token>findAllByUserIdAndRevokedFalse(Long userId);
    List<Token>findAllByUserId(Long userId);

    @Query("SELECT ut FROM Token ut WHERE ut.userId = :userId AND ut.revoked = false AND ut.expiresAt > :now")
    List<Token> findActiveTokensByUserId(@Param("userId") Long userId,
                                        @Param("now")LocalDateTime now);

    @Modifying
    @Query("update Token tk SET tk.revoked = true WHERE tk.userId = :userId AND tk.expiresAt < :now")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("select count(tk) from Token tk where tk.userId = :userId AND tk.revoked = false ")
    void revokedExpiredTokens(@Param("now") LocalDateTime now);

    @Query("select count(tk) from Token tk where tk.userId = :userId AND tk.revoked = false")
    long countActiveTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Token tk WHERE tk.expiresAt < :cutoffDate")
    void deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
}
