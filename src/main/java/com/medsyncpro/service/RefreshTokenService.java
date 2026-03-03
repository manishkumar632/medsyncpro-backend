package com.medsyncpro.service;

import com.medsyncpro.entity.RefreshToken;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private static final long REFRESH_EXPIRY_DAYS = 7;

    // ─────────────────────────────────────────────
    // CREATE NEW REFRESH TOKEN
    // ─────────────────────────────────────────────

    public String createRefreshToken(User user, String deviceInfo) {

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setExpiryDate(
                Instant.now().plus(REFRESH_EXPIRY_DAYS, ChronoUnit.DAYS));

        refreshTokenRepository.save(refreshToken);

        return rawToken; // send raw token to client
    }

    // ─────────────────────────────────────────────
    // VALIDATE & ROTATE
    // ─────────────────────────────────────────────

    public User validateAndRotate(String rawToken) {

        String tokenHash = hash(rawToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN",
                        "Refresh token invalid"));

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BusinessException("REFRESH_TOKEN_EXPIRED",
                    "Refresh token expired");
        }

        // ROTATION (important security step)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return storedToken.getUser();
    }

    // ─────────────────────────────────────────────
    // LOGOUT SINGLE DEVICE
    // ─────────────────────────────────────────────

    public void revokeToken(String rawToken) {
        String tokenHash = hash(rawToken);
        refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ─────────────────────────────────────────────
    // LOGOUT ALL DEVICES
    // ─────────────────────────────────────────────

    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    // ─────────────────────────────────────────────
    // HASHING
    // ─────────────────────────────────────────────

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token");
        }
    }
}