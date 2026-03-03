package com.medsyncpro.controller;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.response.DocumentTypeConfigResponse;
import com.medsyncpro.entity.UserModelType;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/document-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    /**
     * GET /api/admin/document-types/{modelType}
     * List all document types configured for a model (including inactive).
     */
    @GetMapping("/{modelType}")
    public ResponseEntity<ApiResponse<List<DocumentTypeConfigResponse>>> getDocumentTypes(
            @PathVariable String modelType) {

        UserModelType type = UserModelType.valueOf(modelType.toUpperCase());
        List<DocumentTypeConfigResponse> configs = documentTypeService.getDocumentTypesForModel(type);

        return ResponseEntity.ok(
                ApiResponse.success(configs, "Document types retrieved for " + type));
    }

    /**
     * POST /api/admin/document-types
     * Create a new document type and assign it to a model.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentTypeConfigResponse>> createDocumentType(
            @RequestBody DocumentTypeRequest request) {

        DocumentTypeConfigResponse config = documentTypeService.createDocumentType(request);

        return ResponseEntity.ok(
                ApiResponse.success(config, "Document type created and assigned successfully"));
    }

    /**
     * DELETE /api/admin/document-types/mapping/{id}
     * Remove a document type from a model (soft delete mapping).
     */
    @DeleteMapping("/mapping/{id}")
    public ResponseEntity<ApiResponse<Void>> removeMapping(@PathVariable Long id) {

        documentTypeService.removeDocumentTypeFromModel(id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Document type mapping removed"));
    }

    /**
     * PATCH /api/admin/document-types/mapping/{id}/toggle-required
     */
    @PatchMapping("/mapping/{id}/toggle-required")
    public ResponseEntity<ApiResponse<DocumentTypeConfigResponse>> toggleRequired(
            @PathVariable Long id) {

        DocumentTypeConfigResponse config = documentTypeService.toggleRequired(id);

        return ResponseEntity.ok(
                ApiResponse.success(config, "Required status toggled"));
    }

    /**
     * PATCH /api/admin/document-types/mapping/{id}/toggle-active
     */
    @PatchMapping("/mapping/{id}/toggle-active")
    public ResponseEntity<ApiResponse<DocumentTypeConfigResponse>> toggleActive(
            @PathVariable Long id) {

        DocumentTypeConfigResponse config = documentTypeService.toggleActive(id);

        return ResponseEntity.ok(
                ApiResponse.success(config, "Active status toggled"));
    }

    /**
     * DELETE /api/admin/document-types/{id}
     * Soft delete a document type globally (fails if user documents exist).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable Long id) {

        documentTypeService.deleteDocumentType(id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Document type deleted"));
    }
}
