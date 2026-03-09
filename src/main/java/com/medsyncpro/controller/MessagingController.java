package com.medsyncpro.controller;

import com.medsyncpro.dto.request.SendMessageRequest;
import com.medsyncpro.dto.response.ChatMessageResponse;
import com.medsyncpro.dto.response.ConversationResponse;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
public class MessagingController {

    private final MessagingService messagingService;
    private final UserRepository userRepository;

    // ─── List conversations ────────────────────────────────────────────────────

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            Authentication authentication) {
        User user = getUser(authentication);
        List<ConversationResponse> list = user.getRole() == Role.DOCTOR
                ? messagingService.getConversationsForDoctor(user.getId())
                : messagingService.getConversationsForPatient(user.getId());
        return ResponseEntity.ok(ApiResponse.success(list, "Conversations retrieved"));
    }

    // ─── Start or resume a conversation ───────────────────────────────────────
    /**
     * PATIENT sends: { "doctorId": "<Doctor entity UUID>" }
     * DOCTOR sends: { "patientId": "<Patient entity UUID>" }
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        User user = getUser(authentication);
        try {
            ConversationResponse conv;
            if (user.getRole() == Role.PATIENT) {
                UUID doctorId = UUID.fromString(requireParam(body, "doctorId"));
                conv = messagingService.getOrCreateConversationAsPatient(user.getId(), doctorId);
            } else {
                UUID patientId = UUID.fromString(requireParam(body, "patientId"));
                conv = messagingService.getOrCreateConversationAsDoctor(user.getId(), patientId);
            }
            return ResponseEntity.ok(ApiResponse.success(conv, "Conversation ready"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[Messaging] startConversation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to start conversation"));
        }
    }

    // ─── Get paginated messages ────────────────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        User user = getUser(authentication);
        size = Math.min(size, 100);
        try {
            Page<ChatMessageResponse> messages = messagingService.getMessages(
                    conversationId, user.getId(), PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success(messages, "Messages retrieved"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Send a message ────────────────────────────────────────────────────────

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {

        User user = getUser(authentication);
        try {
            ChatMessageResponse msg = messagingService.sendMessage(
                    conversationId, user.getId(), request.getContent());
            return ResponseEntity.ok(ApiResponse.success(msg, "Message sent"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[Messaging] sendMessage error — conv={}: {}", conversationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to send message"));
        }
    }

    // ─── Mark conversation as read ─────────────────────────────────────────────

    @PatchMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable UUID conversationId) {
        User user = getUser(authentication);
        try {
            messagingService.markConversationAsRead(conversationId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Total unread count (navbar badge) ────────────────────────────────────

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount(
            Authentication authentication) {
        User user = getUser(authentication);
        int count = user.getRole() == Role.DOCTOR
                ? messagingService.getTotalUnreadForDoctor(user.getId())
                : messagingService.getTotalUnreadForPatient(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count), "OK"));
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireParam(Map<String, String> body, String key) {
        if (body == null)
            throw new IllegalArgumentException(key + " is required");
        String val = body.get(key);
        if (val == null || val.isBlank())
            throw new IllegalArgumentException(key + " is required");
        return val;
    }
}