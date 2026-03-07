package com.medsyncpro.service;

import com.medsyncpro.entity.AuditLog;
import com.medsyncpro.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(UUID userId, String action, String metadata) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .metadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit Logged: User={}, Action={}", userId, action);
        } catch (Exception e) {
            log.error("Failed to save audit log for user {} action {}", userId, action, e);
        }
    }
}
