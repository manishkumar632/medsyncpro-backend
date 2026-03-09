package com.medsyncpro.service;

import com.medsyncpro.dto.request.PharmacyAssignAgentRequest;
import com.medsyncpro.dto.request.PharmacyMedicineRequestCreateRequest;
import com.medsyncpro.dto.request.PharmacyRequestStatusUpdateRequest;
import com.medsyncpro.dto.response.PharmacyMedicineRequestResponse;
import com.medsyncpro.dto.response.PharmacySearchResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PharmacyWorkflowService {

    private static final Set<PharmacyRequestStatus> PHARMACY_ALLOWED_STATUSES = EnumSet.of(
            PharmacyRequestStatus.APPROVED,
            PharmacyRequestStatus.REJECTED,
            PharmacyRequestStatus.PROCESSING,
            PharmacyRequestStatus.READY_FOR_DISPATCH,
            PharmacyRequestStatus.CANCELLED
    );

    private static final Set<PharmacyRequestStatus> AGENT_ALLOWED_STATUSES = EnumSet.of(
            PharmacyRequestStatus.OUT_FOR_DELIVERY,
            PharmacyRequestStatus.DELIVERED
    );

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacyRepository pharmacyRepository;
    private final AgentRepository agentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyMedicineRequestRepository pharmacyMedicineRequestRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional(readOnly = true)
    public Page<PharmacySearchResponse> searchPharmacies(String q, String location, Pageable pageable) {
        return pharmacyRepository.searchVerifiedPharmacies(q, location, pageable)
                .map(this::toSearchResponse);
    }

    @Transactional
    public PharmacyMedicineRequestResponse createMedicineRequest(
            UUID patientUserId,
            PharmacyMedicineRequestCreateRequest request) {
        Patient patient = getPatientByUserId(patientUserId);

        Pharmacy pharmacy = pharmacyRepository.findByIdAndDeletedFalse(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found"));
        if (!pharmacy.isVerified()) {
            throw new IllegalArgumentException("Pharmacy is not verified");
        }

        Prescription prescription = null;
        if (request.getPrescriptionId() != null) {
            prescription = prescriptionRepository.findById(request.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
            if (!prescription.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Prescription does not belong to this patient");
            }
        }

        PharmacyMedicineRequest medicineRequest = PharmacyMedicineRequest.builder()
                .patient(patient)
                .pharmacy(pharmacy)
                .prescription(prescription)
                .status(PharmacyRequestStatus.REQUESTED)
                .patientNote(request.getNote())
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        medicineRequest = pharmacyMedicineRequestRepository.save(medicineRequest);

        notificationDispatchService.notifyUser(
                pharmacy.getUser(),
                "PHARMACY_REQUEST",
                "New Medicine Request",
                "A patient has sent a medicine request to your pharmacy.",
                medicineRequest.getId().toString(),
                true,
                false);

        return toResponse(medicineRequest);
    }

    @Transactional(readOnly = true)
    public Page<PharmacyMedicineRequestResponse> getPatientRequests(UUID patientUserId, Pageable pageable) {
        Patient patient = getPatientByUserId(patientUserId);
        return pharmacyMedicineRequestRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PharmacyMedicineRequestResponse> getPharmacyRequests(UUID pharmacyUserId, Pageable pageable) {
        Pharmacy pharmacy = getPharmacyByUserId(pharmacyUserId);
        return pharmacyMedicineRequestRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacy.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PharmacyMedicineRequestResponse updatePharmacyRequestStatus(
            UUID pharmacyUserId,
            UUID requestId,
            PharmacyRequestStatusUpdateRequest request) {
        Pharmacy pharmacy = getPharmacyByUserId(pharmacyUserId);
        PharmacyMedicineRequest medicineRequest = pharmacyMedicineRequestRepository
                .findByIdAndPharmacyId(requestId, pharmacy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!PHARMACY_ALLOWED_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Pharmacy cannot set this status");
        }

        medicineRequest.setStatus(request.getStatus());
        medicineRequest.setPharmacyNote(request.getNote());
        if (request.getStatus() == PharmacyRequestStatus.DELIVERED) {
            medicineRequest.setDeliveredAt(LocalDateTime.now());
        }

        medicineRequest = pharmacyMedicineRequestRepository.save(medicineRequest);

        notificationDispatchService.notifyUser(
                medicineRequest.getPatient().getUser(),
                "PHARMACY_REQUEST_UPDATE",
                "Medicine Request Updated",
                "Your medicine request status is now " + request.getStatus().name(),
                medicineRequest.getId().toString(),
                true,
                true);

        return toResponse(medicineRequest);
    }

    @Transactional
    public PharmacyMedicineRequestResponse assignAgent(
            UUID pharmacyUserId,
            UUID requestId,
            PharmacyAssignAgentRequest request) {
        Pharmacy pharmacy = getPharmacyByUserId(pharmacyUserId);
        PharmacyMedicineRequest medicineRequest = pharmacyMedicineRequestRepository
                .findByIdAndPharmacyId(requestId, pharmacy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        Agent agent = agentRepository.findByIdAndDeletedFalse(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        if (!agent.isVerified()) {
            throw new IllegalArgumentException("Assigned agent is not verified");
        }

        medicineRequest.setAgent(agent);
        medicineRequest.setAssignedAt(LocalDateTime.now());
        medicineRequest.setStatus(PharmacyRequestStatus.READY_FOR_DISPATCH);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            medicineRequest.setPharmacyNote(request.getNote());
        }

        medicineRequest = pharmacyMedicineRequestRepository.save(medicineRequest);

        notificationDispatchService.notifyUser(
                agent.getUser(),
                "AGENT_ASSIGNMENT",
                "New Delivery Assignment",
                "You have been assigned a medicine delivery request.",
                medicineRequest.getId().toString(),
                true,
                true);

        notificationDispatchService.notifyUser(
                medicineRequest.getPatient().getUser(),
                "PHARMACY_REQUEST_UPDATE",
                "Delivery Agent Assigned",
                "A delivery agent has been assigned to your medicine request.",
                medicineRequest.getId().toString(),
                true,
                false);

        return toResponse(medicineRequest);
    }

    @Transactional(readOnly = true)
    public Page<PharmacyMedicineRequestResponse> getAgentRequests(UUID agentUserId, Pageable pageable) {
        Agent agent = getAgentByUserId(agentUserId);
        return pharmacyMedicineRequestRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PharmacyMedicineRequestResponse updateAgentRequestStatus(
            UUID agentUserId,
            UUID requestId,
            PharmacyRequestStatusUpdateRequest request) {
        Agent agent = getAgentByUserId(agentUserId);
        PharmacyMedicineRequest medicineRequest = pharmacyMedicineRequestRepository
                .findByIdAndAgentId(requestId, agent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!AGENT_ALLOWED_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Agent cannot set this status");
        }

        medicineRequest.setStatus(request.getStatus());
        if (request.getStatus() == PharmacyRequestStatus.DELIVERED) {
            medicineRequest.setDeliveredAt(LocalDateTime.now());
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            medicineRequest.setPharmacyNote(request.getNote());
        }

        medicineRequest = pharmacyMedicineRequestRepository.save(medicineRequest);

        notificationDispatchService.notifyUser(
                medicineRequest.getPatient().getUser(),
                "DELIVERY_UPDATE",
                "Delivery Status Updated",
                "Your medicine request is now " + request.getStatus().name(),
                medicineRequest.getId().toString(),
                true,
                true);

        notificationDispatchService.notifyUser(
                medicineRequest.getPharmacy().getUser(),
                "DELIVERY_UPDATE",
                "Delivery Progress Update",
                "Assigned agent updated request status to " + request.getStatus().name(),
                medicineRequest.getId().toString(),
                true,
                false);

        return toResponse(medicineRequest);
    }

    private Patient getPatientByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    private Pharmacy getPharmacyByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return pharmacyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy profile not found"));
    }

    private Agent getAgentByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return agentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));
    }

    private PharmacySearchResponse toSearchResponse(Pharmacy pharmacy) {
        return PharmacySearchResponse.builder()
                .id(pharmacy.getId())
                .name(pharmacy.getName())
                .address(pharmacy.getAddress())
                .profileImage(pharmacy.getProfileImage())
                .verified(pharmacy.isVerified())
                .build();
    }

    private PharmacyMedicineRequestResponse toResponse(PharmacyMedicineRequest request) {
        return PharmacyMedicineRequestResponse.builder()
                .id(request.getId())
                .status(request.getStatus())
                .patientId(request.getPatient().getId())
                .patientName(request.getPatient().getName())
                .pharmacyId(request.getPharmacy().getId())
                .pharmacyName(request.getPharmacy().getName())
                .prescriptionId(request.getPrescription() != null ? request.getPrescription().getId() : null)
                .agentId(request.getAgent() != null ? request.getAgent().getId() : null)
                .agentName(request.getAgent() != null ? request.getAgent().getName() : null)
                .patientNote(request.getPatientNote())
                .pharmacyNote(request.getPharmacyNote())
                .deliveryAddress(request.getDeliveryAddress())
                .assignedAt(request.getAssignedAt())
                .deliveredAt(request.getDeliveredAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
