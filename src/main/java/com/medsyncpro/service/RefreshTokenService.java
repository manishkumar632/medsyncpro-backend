package com.medsyncpro.service;

import com.medsyncpro.entity.RefreshToken;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;
    
    /**
     * Create a new refresh token for a user + device combination.
     * Revokes any existing token for the same device first.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo) {
        // Revoke existing tokens for this device
        refreshTokenRepository.revokeByUserAndDevice(user, deviceInfo);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setRevoked(false);
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user {} on device {}", user.getEmail(), deviceInfo);
        return refreshToken;
    }
    
    /**
     * Verify a refresh token and return it if valid.
     * Throws if expired, revoked, or not found.
     */
    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or has been revoked"));
        
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired. Please login again");
        }
        
        return refreshToken;
    }
    
    /**
     * Rotate refresh token: revoke old, create new for same device.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser(), oldToken.getDeviceInfo());
    }
    
    /**
     * Revoke all refresh tokens for a specific device.
     */
    @Transactional
    public void revokeByDevice(User user, String deviceInfo) {
        int revoked = refreshTokenRepository.revokeByUserAndDevice(user, deviceInfo);
        log.info("Revoked {} refresh tokens for user {} on device {}", revoked, user.getEmail(), deviceInfo);
    }
    
    /**
     * Revoke ALL refresh tokens for a user (logout all devices).
     */
    @Transactional
    public void revokeAllForUser(User user) {
        int revoked = refreshTokenRepository.revokeAllByUser(user);
        log.info("Revoked {} refresh tokens for user {} (all devices)", revoked, user.getEmail());
    }
    
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
    
    /**
     * Cleanup expired/revoked tokens every 6 hours.
     */
    @Scheduled(fixedRate = 21600000) // Every 6 hours
    @Transactional
    public void cleanupExpired() {
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}
