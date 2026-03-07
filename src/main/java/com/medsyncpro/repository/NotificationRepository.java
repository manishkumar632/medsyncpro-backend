package com.medsyncpro.repository;

import com.medsyncpro.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // Admins: own notifications + broadcast (recipientId IS NULL)
    List<Notification> findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(
            String recipientId); // ← was UUID

    // Other roles: only their own notifications
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(
            String recipientId); // ← was UUID

    // Unread count for a specific user
    long countByRecipientIdAndIsReadFalse(
            String recipientId); // ← was UUID

    // Unread count for broadcast notifications (admin)
    long countByRecipientIdIsNullAndIsReadFalse();
}