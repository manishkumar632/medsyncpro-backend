package com.medsyncpro.service;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.response.DocumentTypeConfigResponse;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.DocumentTypeEntity;
import com.medsyncpro.entity.ModelDocumentType;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.UserModelType;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DocumentRepository;
import com.medsyncpro.repository.DocumentTypeEntityRepository;
import com.medsyncpro.repository.ModelDocumentTypeRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeService {

    private final DocumentTypeEntityRepository documentTypeRepository;
    private final ModelDocumentTypeRepository modelDocumentTypeRepository;
    private final DocumentRepository documentRepository;

    // ── Admin: list all doc-type configs for a model ──────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentTypeConfigResponse> getDocumentTypesForModel(UserModelType modelType) {
        return modelDocumentTypeRepository
                .findByModelTypeAndDeletedFalseOrderByDisplayOrderAsc(modelType)
                .stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }

    // ── User-facing: only active configs ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentTypeConfigResponse> getActiveDocumentTypesForModel(UserModelType modelType) {
        return modelDocumentTypeRepository
                .findByModelTypeAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(modelType)
                .stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }

    // ── Create & assign ──────────────────────────────────────────────────────

    // @Transactional
    // public DocumentTypeConfigResponse createDocumentType(HttpServletRequest request, HttpServletResponse response, DocumentTypeRequest req) {
    //     Role role = request.getRole();
    //     String code = toCode(request.getName());
    //     // Check duplicate name (case-insensitive) for this model
    //     DocumentType existingType = documentTypeRepository
    //             .findByCodeAndDeletedFalse(code)
    //             .orElse(null);

    //     if (existingType != null) {
    //         // Type exists globally; check if already assigned to this model
    //         boolean alreadyAssigned = modelDocumentTypeRepository
    //                 .existsByModelTypeAndDocumentTypeAndDeletedFalse(modelType, existingType);
    //         if (alreadyAssigned) {
    //             throw new BusinessException("DUPLICATE_DOCUMENT_TYPE",
    //                     "Document type '" + request.getName() + "' is already assigned to " + modelType);
    //         }
    //     }

    //     // Create or reuse the DocumentTypeEntity
    //     DocumentTypeEntity docType;
    //     if (existingType != null) {
    //         docType = existingType;
    //     } else {
    //         docType = DocumentTypeEntity.builder()
    //                 .name(request.getName().trim())
    //                 .code(code)
    //                 .description(request.getDescription())
    //                 .active(true)
    //                 .build();
    //         docType = documentTypeRepository.save(docType);
    //     }

    //     // Create model mapping
    //     ModelDocumentType mapping = ModelDocumentType.builder()
    //             .modelType(modelType)
    //             .documentType(docType)
    //             .required(request.isRequired())
    //             .active(true)
    //             .displayOrder(request.getDisplayOrder())
    //             .build();
    //     mapping = modelDocumentTypeRepository.save(mapping);

    //     log.info("Created document type '{}' and assigned to {}", docType.getName(), modelType);
    //     return toConfigResponse(mapping);
    // }

    // ── Remove mapping (soft delete) ─────────────────────────────────────────

    @Transactional
    public void removeDocumentTypeFromModel(Long mappingId) {
        ModelDocumentType mapping = modelDocumentTypeRepository.findByIdAndDeletedFalse(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Document type mapping not found"));

        mapping.setDeleted(true);
        modelDocumentTypeRepository.save(mapping);

        log.info("Removed document type mapping {} (type: {}, model: {})",
                mappingId, mapping.getDocumentType().getName(), mapping.getModelType());
    }

    // ── Toggle required/optional ─────────────────────────────────────────────

    @Transactional
    public DocumentTypeConfigResponse toggleRequired(Long mappingId) {
        ModelDocumentType mapping = modelDocumentTypeRepository.findByIdAndDeletedFalse(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Document type mapping not found"));

        mapping.setRequired(!mapping.isRequired());
        mapping = modelDocumentTypeRepository.save(mapping);

        log.info("Toggled required for mapping {} to {}", mappingId, mapping.isRequired());
        return toConfigResponse(mapping);
    }

    // ── Toggle active/inactive ───────────────────────────────────────────────

    @Transactional
    public DocumentTypeConfigResponse toggleActive(Long mappingId) {
        ModelDocumentType mapping = modelDocumentTypeRepository.findByIdAndDeletedFalse(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Document type mapping not found"));

        mapping.setActive(!mapping.isActive());
        mapping = modelDocumentTypeRepository.save(mapping);

        log.info("Toggled active for mapping {} to {}", mappingId, mapping.isActive());
        return toConfigResponse(mapping);
    }

    // ── Rename a document type (via mapping) ─────────────────────────────────

    // @Transactional
    // public DocumentTypeConfigResponse renameDocumentType(Long mappingId, String newName) {
    //     if (newName == null || newName.trim().isEmpty()) {
    //         throw new BusinessException("INVALID_NAME", "Document type name cannot be empty");
    //     }
    //     newName = newName.trim();
    //     if (newName.length() > 100) {
    //         throw new BusinessException("INVALID_NAME", "Name must be at most 100 characters");
    //     }

    //     ModelDocumentType mapping = modelDocumentTypeRepository.findByIdAndDeletedFalse(mappingId)
    //             .orElseThrow(() -> new ResourceNotFoundException("Document type mapping not found"));

    //     DocumentTypeEntity docType = mapping.getDocumentType();
    //     String newCode = toCode(newName);

    //     // Check if new code conflicts with another existing type
    //     documentTypeRepository.findByCodeAndDeletedFalse(newCode).ifPresent(existing -> {
    //         if (!existing.getId().equals(docType.getId())) {
    //             throw new BusinessException("DUPLICATE_DOCUMENT_TYPE",
    //                     "A document type with name '" + newName + "' already exists");
    //         }
    //     });

    //     docType.setName(newName);
    //     docType.setCode(newCode);
    //     documentTypeRepository.save(docType);

    //     log.info("Renamed document type {} to '{}'", docType.getId(), newName);
    //     return toConfigResponse(mapping);
    // }

    // ── Soft delete a document type globally ─────────────────────────────────

    @Transactional
    public void deleteDocumentType(UUID documentTypeId) {
        DocumentTypeEntity docType = documentTypeRepository.findByIdAndDeletedFalse(documentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

        // Prevent deletion if user documents exist for this type
        if (documentRepository.existsByDocumentType(docType)) {
            throw new BusinessException("HAS_USER_DOCUMENTS",
                    "Cannot delete document type '" + docType.getName()
                            + "' — user documents exist. Deactivate it instead.");
        }

        // Soft delete all mappings for this type
        List<ModelDocumentType> mappings = modelDocumentTypeRepository
                .findByDocumentTypeAndDeletedFalse(docType);
        for (ModelDocumentType mapping : mappings) {
            mapping.setDeleted(true);
            modelDocumentTypeRepository.save(mapping);
        }

        // Soft delete the type itself
        docType.setDeleted(true);
        documentTypeRepository.save(docType);

        log.info("Soft deleted document type '{}' (id: {})", docType.getName(), documentTypeId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DocumentTypeConfigResponse toConfigResponse(ModelDocumentType mapping) {
        DocumentTypeEntity dt = mapping.getDocumentType();
        return DocumentTypeConfigResponse.builder()
                .id(mapping.getId())
                .documentTypeId(dt.getId())
                .modelType(mapping.getModelType().name())
                .name(dt.getName())
                .code(dt.getCode())
                .description(dt.getDescription())
                .required(mapping.isRequired())
                .active(mapping.isActive())
                .displayOrder(mapping.getDisplayOrder())
                .createdAt(mapping.getCreatedAt())
                .build();
    }

    private String toCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private UserModelType parseModelType(String value) {
        try {
            return UserModelType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_MODEL_TYPE",
                    "Invalid model type: " + value + ". Allowed: DOCTOR, PHARMACIST, AGENT, PATIENT");
        }
    }
}
