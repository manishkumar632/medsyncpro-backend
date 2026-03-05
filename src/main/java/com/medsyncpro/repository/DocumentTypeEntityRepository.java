package com.medsyncpro.repository;

import com.medsyncpro.entity.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTypeEntityRepository extends JpaRepository<DocumentTypeEntity, UUID> {

    List<DocumentTypeEntity> findByDeletedFalse();

    Optional<DocumentTypeEntity> findByCodeAndDeletedFalse(String code);

    Optional<DocumentTypeEntity> findByIdAndDeletedFalse(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByCodeAndDeletedFalse(String code);
}
