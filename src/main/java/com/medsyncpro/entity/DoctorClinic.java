package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_clinics", indexes = {
    @Index(name = "idx_dc_user_id", columnList = "userId")
})
@Data
public class DoctorClinic {
    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @PrePersist
    public void generateId() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(length = 200)
    private String clinicName;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(nullable = false)
    private Boolean isPrimary = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
