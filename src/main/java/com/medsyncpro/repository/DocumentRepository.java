package com.medsyncpro.repository;

import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(String userId);
    Optional<Document> findByUserIdAndType(String userId, DocumentType type);
    void deleteByUserIdAndId(String userId, Long id);
    void deleteByUserIdAndType(String userId, DocumentType type);
}
