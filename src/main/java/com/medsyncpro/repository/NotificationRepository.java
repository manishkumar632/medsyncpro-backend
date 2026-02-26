package com.medsyncpro.repository;

import com.medsyncpro.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    long countByRecipientIdAndIsReadFalse(String recipientId);
    long countByRecipientIdIsNullAndIsReadFalse();
}
