package com.cosmetics.server.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_tokens",
        indexes = {
                @Index(name = "idx_user_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_user_tokens_token_id", columnList = "token_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Hash của phần "secret" (bcrypt/argon2) — KHÔNG lưu secret plain
    @Column(name = "refresh_token_hash", nullable = false, length = 512)
    private String refreshTokenHash;

    @Column(name = "ip", length = 64)
    private String ip;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
