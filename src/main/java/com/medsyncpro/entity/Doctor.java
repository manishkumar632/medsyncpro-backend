package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "doctors", indexes = {
        @Index(name = "idx_is_doctor_verified", columnList = "isVerified"),
        @Index(name = "idx_speciality_id", columnList = "speciality_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE doctors SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class Doctor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speciality_id", nullable = false)
    private Speciality specialization;
    
    private String licenseNumber;
    
    private Integer experienceYears;
    
    private String qualification;
    
    private String clinicAddress;
    
    private String profileImage;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    private Double consultationFee;

    @Builder.Default
    @Column(nullable = false)
    private boolean isVerified = false;

    
}
