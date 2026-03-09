package com.medsyncpro.repository;

import com.medsyncpro.entity.ChatMessage;
import com.medsyncpro.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Paginated messages ordered oldest → newest (caller scrolls upward to load
     * more).
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    /** Bulk-mark all unread messages from a specific sender role as read. */
    @Modifying
    @Query("""
            UPDATE ChatMessage m
               SET m.isRead = true, m.readAt = :now
             WHERE m.conversation.id = :conversationId
               AND m.senderRole     = :senderRole
               AND m.isRead         = false
            """)
    int markAllAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("senderRole") Role senderRole,
            @Param("now") LocalDateTime now);
}