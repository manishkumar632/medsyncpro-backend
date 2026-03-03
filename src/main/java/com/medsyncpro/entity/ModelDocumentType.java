package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Maps a DocumentTypeEntity to a specific UserModelType.
 * Each model can have its own set of required/optional documents.
 */
@Entity
@Table(name = "model_document_types", indexes = {
        @Index(name = "idx_mdt_model_type", columnList = "modelType"),
        @Index(name = "idx_mdt_active", columnList = "active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_document_type", columnNames = { "modelType", "document_type_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE model_document_types SET deleted = true WHERE id=? AND version=?")
@SQLRestriction("deleted = false")
public class ModelDocumentType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserModelType modelType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentTypeEntity documentType;

    @Builder.Default
    @Column(nullable = false)
    private boolean required = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column
    private Integer displayOrder;
}
