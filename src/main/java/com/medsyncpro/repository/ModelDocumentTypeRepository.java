package com.medsyncpro.repository;

import com.medsyncpro.entity.DocumentTypeEntity;
import com.medsyncpro.entity.ModelDocumentType;
import com.medsyncpro.entity.UserModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelDocumentTypeRepository extends JpaRepository<ModelDocumentType, Long> {

    List<ModelDocumentType> findByModelTypeAndDeletedFalseOrderByDisplayOrderAsc(UserModelType modelType);

    List<ModelDocumentType> findByModelTypeAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UserModelType modelType);

    boolean existsByModelTypeAndDocumentTypeAndDeletedFalse(UserModelType modelType, DocumentTypeEntity documentType);

    Optional<ModelDocumentType> findByModelTypeAndDocumentTypeAndDeletedFalse(
            UserModelType modelType, DocumentTypeEntity documentType);

    Optional<ModelDocumentType> findByIdAndDeletedFalse(Long id);

    List<ModelDocumentType> findByDocumentTypeAndDeletedFalse(DocumentTypeEntity documentType);
}
