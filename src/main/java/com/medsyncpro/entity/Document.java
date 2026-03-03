package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_user_id", columnList = "user_id"),
        @Index(name = "idx_document_type_id", columnList = "document_type_id")
})
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentTypeEntity documentType;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 20)
    private String status = "UPLOADED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
