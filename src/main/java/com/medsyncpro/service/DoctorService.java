package com.medsyncpro.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsyncpro.dto.request.DoctorDocumentUploadRequest;
import com.medsyncpro.dto.response.AppointmentResponse;
import com.medsyncpro.dto.response.DoctorProfileResponseDTO;
import com.medsyncpro.dto.response.RequiredDocumentItem;
import com.medsyncpro.dto.response.SignatureResponseDTO;
import com.medsyncpro.dto.response.VerificationStatusResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.event.AppointmentCancelledEvent;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import com.medsyncpro.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

        private final UserRepository userRepository;
        private final DoctorRepository doctorRepository;
        private final DocumentTypeRepository documentTypeRepository;
        private final DocumentRepository documentRepository;
        private final VerificationRequestRepository verificationRequestRepository;
        private final AppointmentRepository appointmentRepository;
        private final DoctorSettingsRepository doctorSettingsRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final AuditLogService auditLogService;
        private final PrescriptionRepository prescriptionRepository;

        private final FileStorageService fileStorageService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        // ─── Profile ─────────────────────────────────────────────────────────────

        public ApiResponse<DoctorProfileResponseDTO> getDoctorProfileData(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Doctor doctor = doctorRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

                DoctorProfileResponseDTO dto = DoctorProfileResponseDTO.builder()
                                .email(user.getEmail())
                                .phone(user.getPhone() != null ? user.getPhone() : "")
                                .emailVerified(user.isEmailVerified())
                                .phoneVerified(user.isPhoneVerified())
                                .doctorId(doctor.getId())
                                .name(doctor.getName())
                                .licenseNumber(doctor.getLicenseNumber() != null ? doctor.getLicenseNumber() : "")
                                .experienceYears(doctor.getExperienceYears())
                                .clinicName(doctor.getClinicAddress() != null ? doctor.getClinicAddress() : "")
                                .qualification(doctor.getQualification())
                                .clinicAddress(doctor.getClinicAddress())
                                .profileImage(doctor.getProfileImage())
                                .bio(doctor.getBio())
                                .consultationFee(doctor.getConsultationFee())
                                .isVerified(doctor.isVerified())
                                .specializationId(doctor.getSpecialization() != null
                                                ? doctor.getSpecialization().getId()
                                                : null)
                                .specializationName(doctor.getSpecialization() != null
                                                ? doctor.getSpecialization().getName()
                                                : null)
                                .build();

                return ApiResponse.success(dto, "Doctor profile retrieved successfully");
        }

        // ─── Appointments ─────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public Page<AppointmentResponse> getDoctorAppointments(UUID doctorUserId, Pageable pageable) {
                Page<Appointment> page = appointmentRepository
                                .findByDoctorUserIdOrderByScheduledDateDescScheduledTimeDesc(doctorUserId, pageable);
                List<AppointmentResponse> responses = page.getContent().stream()
                                .map(this::toAppointmentResponse)
                                .collect(Collectors.toList());
                return new PageImpl<>(responses, pageable, page.getTotalElements());
        }

        @Transactional
        public AppointmentResponse approveAppointment(UUID doctorUserId, UUID appointmentId) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                if (appointment.getStatus() != AppointmentStatus.PENDING) {
                        throw new IllegalArgumentException("Can only approve pending appointments");
                }
                appointment.setStatus(AppointmentStatus.CONFIRMED);
                appointmentRepository.save(appointment);
                return toAppointmentResponse(appointment);
        }

        @Transactional
        public AppointmentResponse rejectAppointment(UUID doctorUserId, UUID appointmentId, String reason) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                if (appointment.getStatus() != AppointmentStatus.PENDING) {
                        throw new IllegalArgumentException("Can only reject pending appointments");
                }
                appointment.setStatus(AppointmentStatus.REJECTED);
                appointment.setCancellationReason(reason);
                appointmentRepository.save(appointment);
                eventPublisher.publishEvent(new AppointmentCancelledEvent(this, appointment, "DOCTOR"));
                return toAppointmentResponse(appointment);
        }

        @Transactional
        public AppointmentResponse completeAppointment(UUID doctorUserId, UUID appointmentId) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                if (appointment.getStatus() != AppointmentStatus.CONFIRMED &&
                                appointment.getStatus() != AppointmentStatus.PENDING) {
                        throw new IllegalArgumentException("Can only complete confirmed or pending appointments");
                }
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointmentRepository.save(appointment);
                return toAppointmentResponse(appointment);
        }

        @Transactional
        public AppointmentResponse cancelAppointment(UUID doctorUserId, UUID appointmentId, String reason) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                                appointment.getStatus() == AppointmentStatus.CANCELLED) {
                        throw new IllegalArgumentException("Cannot cancel this appointment");
                }
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setCancellationReason(reason != null ? reason : "Cancelled by doctor");
                appointmentRepository.save(appointment);
                eventPublisher.publishEvent(new AppointmentCancelledEvent(this, appointment, "DOCTOR"));
                return toAppointmentResponse(appointment);
        }

        @Transactional
        public AppointmentResponse saveNotes(UUID doctorUserId, UUID appointmentId, Map<String, String> body) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                if (body.containsKey("doctorNotes"))
                        appointment.setDoctorNotes(body.get("doctorNotes"));
                if (body.containsKey("diagnosis"))
                        appointment.setDiagnosis(body.get("diagnosis"));
                if (body.containsKey("followUpDate") && body.get("followUpDate") != null) {
                        appointment.setFollowUpDate(LocalDate.parse(body.get("followUpDate")));
                }
                appointmentRepository.save(appointment);
                return toAppointmentResponse(appointment);
        }

        @Transactional
        public AppointmentResponse savePrescription(UUID doctorUserId, UUID appointmentId,
                        Map<String, Object> body) {
                Appointment appointment = getAndValidateAppointment(doctorUserId, appointmentId);
                try {
                        String prescriptionJson = objectMapper.writeValueAsString(body);
                        appointment.setPrescription(prescriptionJson);
                        if (body.containsKey("diagnosis"))
                                appointment.setDiagnosis(String.valueOf(body.get("diagnosis")));
                        if (body.containsKey("notes"))
                                appointment.setDoctorNotes(String.valueOf(body.get("notes")));

                        Prescription prescriptionEntity = prescriptionRepository.findByAppointmentId(appointmentId)
                                        .orElseGet(() -> Prescription.builder()
                                                        .doctor(appointment.getDoctor())
                                                        .patient(appointment.getPatient())
                                                        .appointment(appointment)
                                                        .build());

                        String medicinesJson = body.containsKey("medicines")
                                        ? objectMapper.writeValueAsString(body.get("medicines"))
                                        : prescriptionJson;

                        prescriptionEntity.setMedicines(medicinesJson);
                        prescriptionEntity.setNotes(body.containsKey("notes")
                                        ? String.valueOf(body.get("notes"))
                                        : null);
                        prescriptionRepository.save(prescriptionEntity);
                } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid prescription data");
                }
                appointmentRepository.save(appointment);
                return toAppointmentResponse(appointment);
        }

        // ─── Document Types ───────────────────────────────────────────────────────

        public List<DocumentType> getActiveDocumentTypes() {
                List<DocumentType> all = documentTypeRepository.findByRole(Role.DOCTOR).orElse(List.of());
                log.info("Found {} document types for DOCTOR role", all.size());
                all.forEach(dt -> log.info("  - {} (active={}, required={})",
                                dt.getName(), dt.isActive(), dt.isRequired()));

                List<DocumentType> active = all.stream()
                                .filter(DocumentType::isActive)
                                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                                .collect(Collectors.toList());
                log.info("Returning {} active document types", active.size());
                return active;
        }

        // ─── Cloudinary Upload Signature ──────────────────────────────────────────
        public SignatureResponseDTO generateDocumentUploadSignature(UUID userId, String documentTypeId) {
                // Validate the document type exists and belongs to DOCTOR role
                UUID docTypeUuid;
                try {
                        docTypeUuid = UUID.fromString(documentTypeId);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid documentTypeId format");
                }

                DocumentType docType = documentTypeRepository.findById(docTypeUuid)
                                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

                if (!docType.isActive()) {
                        throw new IllegalArgumentException("Document type is not active");
                }
                if (docType.getRole() != Role.DOCTOR) {
                        throw new IllegalArgumentException("Document type does not belong to the DOCTOR role");
                }

                // Scoped folder: medsyncpro/documents/doctors/<userId>
                String folder = "medsyncpro/documents/doctors/" + userId;

                log.info("Generating Cloudinary upload signature for user={} docType={} folder={}",
                                userId, docType.getCode(), folder);

                return fileStorageService.generateUploadSignature(folder);
        }

        public VerificationStatusResponse getVerificationStatus(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                List<DocumentType> activeTypes = getActiveDocumentTypes();
                List<Document> uploadedDocs = documentRepository.findByUser(user);

                List<RequiredDocumentItem> items = activeTypes.stream().map(docType -> {
                        Document uploaded = uploadedDocs.stream()
                                        .filter(d -> d.getDocumentType() != null
                                                        && d.getDocumentType().getId().equals(docType.getId()))
                                        .findFirst()
                                        .orElse(null);

                        return RequiredDocumentItem.builder()
                                        .documentTypeId(docType.getId())
                                        .typeCode(docType.getCode())
                                        .label(docType.getName())
                                        .description(docType.getDescription())
                                        .required(docType.isRequired())
                                        .uploaded(uploaded != null)
                                        .status(uploaded != null ? uploaded.getStatus() : "NOT_UPLOADED")
                                        .fileUrl(uploaded != null ? uploaded.getFileUrl() : null)
                                        .fileName(uploaded != null ? uploaded.getFileName() : null)
                                        .fileSize(uploaded != null ? uploaded.getFileSize() : null)
                                        .build();
                }).collect(Collectors.toList());

                // ✅ FIX: pass userId (UUID) directly — NOT userId.toString()
                VerificationRequest verReq = verificationRequestRepository
                                .findByUserId(userId) // ← was: findByUserId(userId.toString())
                                .orElse(null);

                return VerificationStatusResponse.builder()
                                .status(verReq != null ? verReq.getStatus() : VerificationStatus.UNVERIFIED)
                                .requiredDocuments(items)
                                .submittedDocuments(List.of())
                                .verificationNotes(verReq != null ? verReq.getReviewNotes() : null)
                                .submittedAt(verReq != null ? verReq.getSubmittedAt() : null)
                                .reviewedAt(verReq != null ? verReq.getReviewedAt() : null)
                                .build();
        }        

        // ─── Save Uploaded Document ───────────────────────────────────────────────

        /**
         * Persists Cloudinary metadata after the browser has successfully uploaded
         * a file directly to Cloudinary.
         */
        @Transactional
        public RequiredDocumentItem saveUploadedDocument(UUID userId, DoctorDocumentUploadRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                DocumentType docType = documentTypeRepository.findById(request.getDocumentTypeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

                if (request.getSecureUrl() == null || request.getSecureUrl().isBlank()) {
                        throw new IllegalArgumentException("Secure URL is required");
                }
                if (request.getPublicId() == null || request.getPublicId().isBlank()) {
                        throw new IllegalArgumentException("Public ID is required");
                }

                // Upsert: replace any previously uploaded version of the same document type
                Document document = documentRepository
                                .findByUserAndDocumentTypeId(user, docType.getId())
                                .orElseGet(() -> {
                                        Document doc = new Document();
                                        doc.setUser(user);
                                        doc.setDocumentType(docType);
                                        return doc;
                                });

                document.setFileUrl(request.getSecureUrl());
                document.setPublicId(request.getPublicId());
                document.setFileName(request.getOriginalFilename());
                document.setFileSize(request.getBytes());
                document.setStatus("UPLOADED");
                documentRepository.save(document);

                log.info("Document saved — user={} docType={}", userId, docType.getCode());
                auditLogService.logAction(userId, "DOCUMENT_UPLOAD",
                                "Uploaded document: " + docType.getName());

                return RequiredDocumentItem.builder()
                                .documentTypeId(docType.getId())
                                .typeCode(docType.getCode())
                                .label(docType.getName())
                                .description(docType.getDescription())
                                .required(docType.isRequired())
                                .uploaded(true)
                                .status("UPLOADED")
                                .fileUrl(document.getFileUrl())
                                .fileName(document.getFileName())
                                .fileSize(document.getFileSize())
                                .build();
        }

        // ─── Submit for Verification ──────────────────────────────────────────────

        @Transactional
        public VerificationStatusResponse submitForVerification(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // Validate all required documents are uploaded
                List<DocumentType> requiredTypes = getActiveDocumentTypes().stream()
                                .filter(DocumentType::isRequired)
                                .collect(Collectors.toList());

                List<Document> uploadedDocs = documentRepository.findByUser(user);

                for (DocumentType requiredType : requiredTypes) {
                        boolean found = uploadedDocs.stream()
                                        .anyMatch(d -> d.getDocumentType() != null
                                                        && d.getDocumentType().getId().equals(requiredType.getId()));
                        if (!found) {
                                throw new IllegalArgumentException(
                                                "Required document missing: " + requiredType.getName());
                        }
                }

                // Create or update verification request
                VerificationRequest verReq = verificationRequestRepository
                                .findByUserId(userId) // ← UUID directly, NOT .toString()
                                .orElseGet(() -> {
                                        VerificationRequest vr = new VerificationRequest();
                                        vr.setUser(user);
                                        return vr;
                                });

                verReq.setStatus(VerificationStatus.DOCUMENT_SUBMITTED);
                verReq.setSubmittedAt(LocalDateTime.now());
                verificationRequestRepository.save(verReq);

                eventPublisher.publishEvent(
                                new com.medsyncpro.event.VerificationSubmittedEvent(this, user));

                log.info("Verification submitted — user={}", userId);
                return getVerificationStatus(userId);
        }

        // ─── Private helpers ──────────────────────────────────────────────────────

        private Appointment getAndValidateAppointment(UUID doctorUserId, UUID appointmentId) {
                Appointment appointment = appointmentRepository.findById(appointmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
                if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
                        throw new IllegalArgumentException("You can only manage your own appointments");
                }
                return appointment;
        }

        private AppointmentResponse toAppointmentResponse(Appointment appt) {
                Doctor doctor = appt.getDoctor();
                Patient patient = appt.getPatient();
                User doctorUser = doctor.getUser();
                User patientUser = patient.getUser();

                String specialtyName = null;
                if (doctor.getSpecialization() != null) {
                        specialtyName = doctor.getSpecialization().getName();
                } else {
                        DoctorSettings settings = doctorSettingsRepository
                                        .findByUserId(doctorUser.getId()).orElse(null);
                        if (settings != null)
                                specialtyName = settings.getSpecialty();
                }

                return AppointmentResponse.builder()
                                .id(appt.getId())
                                .doctorId(doctor.getId())
                                .doctorName(doctor.getName())
                                .doctorSpecialty(specialtyName)
                                .doctorProfileImage(doctor.getProfileImage())
                                .patientId(patient.getId())
                                .patientName(patient.getName())
                                .patientEmail(patientUser.getEmail())
                                .patientPhone(patientUser.getPhone())
                                .scheduledDate(appt.getScheduledDate())
                                .scheduledTime(appt.getScheduledTime())
                                .endTime(appt.getEndTime())
                                .type(appt.getType().name())
                                .status(appt.getStatus().name())
                                .symptoms(appt.getSymptoms())
                                .doctorNotes(appt.getDoctorNotes())
                                .diagnosis(appt.getDiagnosis())
                                .followUpDate(appt.getFollowUpDate())
                                .cancellationReason(appt.getCancellationReason())
                                .prescription(appt.getPrescription())
                                .build();
        }
}