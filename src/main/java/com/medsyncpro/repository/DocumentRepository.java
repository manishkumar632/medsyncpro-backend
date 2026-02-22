package com.medsyncpro.repository;

import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
    Optional<Document> findByUserIdAndType(Long userId, DocumentType type);
    void deleteByUserIdAndId(Long userId, Long id);
}
