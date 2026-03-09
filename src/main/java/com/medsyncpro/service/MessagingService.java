package com.medsyncpro.service;

import com.medsyncpro.dto.response.ChatMessageResponse;
import com.medsyncpro.dto.response.ConversationResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;
    private final NotificationDispatchService notificationDispatchService;

    // ─────────────────────────────────────────────────────────────────────────
    // Get or create a conversation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Patient opens a conversation with a specific doctor.
     *
     * @param patientUserId  User.id of the calling patient
     * @param doctorEntityId Doctor.id (entity PK, not User.id)
     */
    @Transactional
    public ConversationResponse getOrCreateConversationAsPatient(UUID patientUserId, UUID doctorEntityId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Doctor doctor = doctorRepository.findById(doctorEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Conversation conv = conversationRepository
                .findByDoctorIdAndPatientId(doctor.getId(), patient.getId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder().doctor(doctor).patient(patient).build()));

        return mapConversation(conv, Role.PATIENT);
    }

    /**
     * Doctor opens a conversation with a specific patient.
     *
     * @param doctorUserId    User.id of the calling doctor
     * @param patientEntityId Patient.id (entity PK, not User.id)
     */
    @Transactional
    public ConversationResponse getOrCreateConversationAsDoctor(UUID doctorUserId, UUID patientEntityId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        Patient patient = patientRepository.findById(patientEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Conversation conv = conversationRepository
                .findByDoctorIdAndPatientId(doctor.getId(), patient.getId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder().doctor(doctor).patient(patient).build()));

        return mapConversation(conv, Role.DOCTOR);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List conversations
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsForPatient(UUID patientUserId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        return conversationRepository
                .findByPatientIdOrderByActivity(patient.getId())
                .stream()
                .map(c -> mapConversation(c, Role.PATIENT))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsForDoctor(UUID doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return conversationRepository
                .findByDoctorIdOrderByActivity(doctor.getId())
                .stream()
                .map(c -> mapConversation(c, Role.DOCTOR))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch messages (paginated)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(UUID conversationId, UUID callerUserId, Pageable pageable) {
        Conversation conv = findConversation(conversationId);
        assertParticipant(conv, callerUserId);
        return chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(this::mapMessage);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send a message
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse sendMessage(UUID conversationId, UUID senderUserId, String content) {

        Conversation conv = findConversation(conversationId);
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role senderRole = sender.getRole();
        assertParticipant(conv, senderUserId);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        // ── Persist ──────────────────────────────────────────────────────────
        ChatMessage msg = ChatMessage.builder()
                .conversation(conv)
                .sender(sender)
                .senderRole(senderRole)
                .content(content.trim())
                .isRead(false)
                .build();
        msg = chatMessageRepository.save(msg);

        // ── Update conversation preview + increment the RECIPIENT'S counter ──
        String preview = content.length() > 120 ? content.substring(0, 120) + "…" : content;
        conv.setLastMessage(preview);
        conv.setLastMessageAt(LocalDateTime.now());

        if (senderRole == Role.DOCTOR) {
            conv.setPatientUnreadCount(conv.getPatientUnreadCount() + 1);
        } else {
            conv.setDoctorUnreadCount(conv.getDoctorUnreadCount() + 1);
        }
        conversationRepository.save(conv);

        // ── Resolve recipient ─────────────────────────────────────────────────
        User recipient;
        String senderDisplayName;

        if (senderRole == Role.DOCTOR) {
            recipient = conv.getPatient().getUser();
            senderDisplayName = conv.getDoctor().getName() != null
                    ? "Dr. " + conv.getDoctor().getName()
                    : sender.getEmail();
        } else {
            recipient = conv.getDoctor().getUser();
            senderDisplayName = conv.getPatient().getName() != null
                    ? conv.getPatient().getName()
                    : sender.getEmail();
        }

        // ── SSE: real-time delivery ───────────────────────────────────────────
        // The "new_message" event travels via the existing Spring Boot SSE stream.
        // The Next.js NotificationContext proxy forwards it to the browser.
        Map<String, Object> ssePayload = new LinkedHashMap<>();
        ssePayload.put("type", "NEW_MESSAGE");
        ssePayload.put("conversationId", conv.getId().toString());
        ssePayload.put("messageId", msg.getId().toString());
        ssePayload.put("senderId", sender.getId().toString());
        ssePayload.put("senderName", senderDisplayName);
        ssePayload.put("senderRole", senderRole.name());
        ssePayload.put("content", content.trim());
        ssePayload.put("preview", preview);
        ssePayload.put("sentAt", msg.getCreatedAt() != null
                ? msg.getCreatedAt().toString()
                : LocalDateTime.now().toString());

        sseEmitterService.sendToUser(recipient.getId(), "new_message", ssePayload);

        // ── FCM push (non-critical — swallow failures so the send never fails) ─
        String pushTitle = "New message from " + senderDisplayName;
        String pushBody = content.length() > 80 ? content.substring(0, 80) + "…" : content;
        try {
            // inApp=false → no Notification row saved (SSE is the real-time channel)
            // email=false → no email for chat messages
            // push=true → FCM push if device token present
            notificationDispatchService.notifyUser(
                    recipient, "NEW_MESSAGE", pushTitle, pushBody,
                    conv.getId().toString(),
                    false, // inApp — skip Notification table
                    false, // email
                    true // push
            );
        } catch (Exception e) {
            log.warn("[Messaging] FCM push failed for user={}: {}", recipient.getId(), e.getMessage());
        }

        log.info("[Messaging] Message sent — conv={} sender={} role={}",
                conversationId, senderUserId, senderRole);

        return mapMessage(msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark conversation as read
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void markConversationAsRead(UUID conversationId, UUID callerUserId) {
        Conversation conv = findConversation(conversationId);
        Role callerRole = resolveRole(conv, callerUserId);

        // Mark messages sent by the OTHER party as read
        Role opposite = (callerRole == Role.DOCTOR) ? Role.PATIENT : Role.DOCTOR;
        int updated = chatMessageRepository.markAllAsRead(
                conversationId, opposite, LocalDateTime.now());

        // Zero out the caller's unread counter
        if (callerRole == Role.DOCTOR) {
            conv.setDoctorUnreadCount(0);
        } else {
            conv.setPatientUnreadCount(0);
        }
        conversationRepository.save(conv);

        log.debug("[Messaging] Marked {} messages as read — conv={} caller={}",
                updated, conversationId, callerUserId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Total unread count (for sidebar badge)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public int getTotalUnreadForPatient(UUID patientUserId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return (int) conversationRepository.sumPatientUnread(patient.getId());
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadForDoctor(UUID doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return (int) conversationRepository.sumDoctorUnread(doctor.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────────────────────

    private ConversationResponse mapConversation(Conversation c, Role callerRole) {
        int unread = (callerRole == Role.DOCTOR)
                ? c.getDoctorUnreadCount()
                : c.getPatientUnreadCount();

        String specialty = null;
        if (c.getDoctor().getSpecialization() != null) {
            specialty = c.getDoctor().getSpecialization().getName();
        }

        return ConversationResponse.builder()
                .id(c.getId())
                .doctorId(c.getDoctor().getId())
                .doctorName(c.getDoctor().getName())
                .doctorSpecialty(specialty)
                .doctorAvatar(c.getDoctor().getProfileImage())
                .patientId(c.getPatient().getId())
                .patientName(c.getPatient().getName())
                .patientAvatar(c.getPatient().getProfileImage())
                .lastMessage(c.getLastMessage())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private ChatMessageResponse mapMessage(ChatMessage m) {
        String senderName;
        String senderAvatar = null;
        Conversation conv = m.getConversation();

        if (m.getSenderRole() == Role.DOCTOR) {
            senderName = conv.getDoctor().getName();
            senderAvatar = conv.getDoctor().getProfileImage();
        } else {
            senderName = conv.getPatient().getName();
            senderAvatar = conv.getPatient().getProfileImage();
        }

        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(conv.getId())
                .senderId(m.getSender().getId())
                .senderRole(m.getSenderRole())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .content(m.getContent())
                .isRead(m.isRead())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Guards & helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Conversation findConversation(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private void assertParticipant(Conversation conv, UUID callerUserId) {
        if (!isParticipant(conv, callerUserId)) {
            throw new SecurityException("Access denied — you are not a participant of this conversation");
        }
    }

    private Role resolveRole(Conversation conv, UUID callerUserId) {
        if (conv.getDoctor().getUser().getId().equals(callerUserId))
            return Role.DOCTOR;
        if (conv.getPatient().getUser().getId().equals(callerUserId))
            return Role.PATIENT;
        throw new SecurityException("Access denied — you are not a participant of this conversation");
    }

    private boolean isParticipant(Conversation conv, UUID callerUserId) {
        return conv.getDoctor().getUser().getId().equals(callerUserId)
                || conv.getPatient().getUser().getId().equals(callerUserId);
    }
}