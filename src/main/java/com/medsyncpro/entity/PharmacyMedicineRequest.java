package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_medicine_requests", indexes = {
        @Index(name = "idx_pharma_request_patient", columnList = "patient_id"),
        @Index(name = "idx_pharma_request_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_pharma_request_agent", columnList = "agent_id"),
        @Index(name = "idx_pharma_request_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE pharmacy_medicine_requests SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class PharmacyMedicineRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PharmacyRequestStatus status = PharmacyRequestStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String patientNote;

    @Column(columnDefinition = "TEXT")
    private String pharmacyNote;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    private LocalDateTime assignedAt;

    private LocalDateTime deliveredAt;
}
