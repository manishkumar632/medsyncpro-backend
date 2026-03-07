package com.medsyncpro.repository;

import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    List<Document> findByUser(User user);
    
    Optional<Document> findByUserAndDocumentType(User user, DocumentType documentType);
    
    Optional<Document> findByUserAndDocumentTypeId(User user, UUID documentTypeId);
    
    void deleteByUserAndDocumentType(User user, DocumentType documentType);
    
    boolean existsByDocumentType(DocumentType documentType);
    
    long countByUser(User user);
}
