package com.medsyncpro.repository;

import com.medsyncpro.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    long countByRecipientIdAndIsReadFalse(UUID recipientId);
    long countByRecipientIdIsNullAndIsReadFalse();
}
