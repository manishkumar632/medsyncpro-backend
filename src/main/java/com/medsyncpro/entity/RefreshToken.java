package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_refresh_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Store HASH only (never raw token)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String deviceInfo;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}