package com.medsyncpro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Role;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
    Optional<DocumentType> findByCode(String code);

    Optional<List<DocumentType>> findByRole(Role role);

    // check if the document type exist by code and role
    boolean existsByCodeAndRole(String code, Role role);
}
