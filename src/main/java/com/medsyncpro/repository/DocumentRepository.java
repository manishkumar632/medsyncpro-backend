package com.medsyncpro.repository;

import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(String userId);

    Optional<Document> findByUserIdAndDocumentType(String userId, DocumentTypeEntity documentType);

    void deleteByUserIdAndId(String userId, Long id);

    void deleteByUserIdAndDocumentType(String userId, DocumentTypeEntity documentType);

    boolean existsByDocumentType(DocumentTypeEntity documentType);
}
