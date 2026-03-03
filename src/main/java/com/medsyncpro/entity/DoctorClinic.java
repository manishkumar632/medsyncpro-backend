package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "doctor_clinics", indexes = {
    @Index(name = "idx_dc_user_id", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE doctor_clinics SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class DoctorClinic extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 200)
    private String clinicName;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPrimary = false;
}
