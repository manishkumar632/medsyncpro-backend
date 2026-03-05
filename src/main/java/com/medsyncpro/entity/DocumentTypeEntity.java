package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "document_types", indexes = {
                @Index(name = "idx_doc_type_code", columnList = "code"),
                @Index(name = "idx_doc_type_active", columnList = "active")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_doc_type_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE document_types SET deleted = true WHERE id=? AND version=?")
@SQLRestriction("deleted = false")
public class DocumentTypeEntity extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(nullable = false, length = 100)
        private String name;

        /** Upper-case machine-readable code, e.g. MEDICAL_LICENSE */
        @Column(nullable = false, unique = true, length = 80)
        private String code;

        @Column(length = 500)
        private String description;

        @Builder.Default
        @Column(nullable = false)
        private boolean active = true;
}
