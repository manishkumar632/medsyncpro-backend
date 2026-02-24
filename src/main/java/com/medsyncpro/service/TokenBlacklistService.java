package com.medsyncpro.service;

import com.medsyncpro.entity.BlacklistedToken;
import com.medsyncpro.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    
    /**
     * Blacklist an access token by its JTI.
     * The token stays blacklisted until it naturally expires.
     */
    @Transactional
    public void blacklist(String jti, Instant expiryDate) {
        if (jti == null || isBlacklisted(jti)) {
            return;
        }
        
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setJti(jti);
        blacklistedToken.setExpiryDate(expiryDate);
        blacklistedTokenRepository.save(blacklistedToken);
        log.info("Blacklisted token JTI: {}", jti);
    }
    
    /**
     * Check if a token JTI is blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        return blacklistedTokenRepository.existsByJti(jti);
    }
    
    /**
     * Cleanup expired blacklisted tokens every hour.
     * Expired tokens don't need to stay in the blacklist since
     * JWT validation will reject them anyway.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpired() {
        int deleted = blacklistedTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired blacklisted tokens", deleted);
        }
    }
}
