package com.medsyncpro.controller;

import com.medsyncpro.dto.LoginResponse;
import com.medsyncpro.dto.ProfileResponse;
import com.medsyncpro.dto.RequiredDocumentItem;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.ProfileService;
import com.medsyncpro.utils.Utils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    private final Utils utils;
    
    
    /**
     * Lightweight session validation endpoint.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(Authentication authentication) {
        User user = utils.getUserFromAuth(authentication);
        LoginResponse response = new LoginResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getProfessionalVerificationStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Session valid"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        String userId = utils.getUserFromAuth(authentication).getId();
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }
    
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            Authentication authentication,
            @RequestPart(value = "profile", required = false) String profileJson,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(required = false) Map<String, String> documentTypes) {
        
        String userId = utils.getUserFromAuth(authentication).getId();
        
        ProfileResponse updatedProfile = profileService.updateProfile(
                userId,
                profileJson,
                profileImage,
                documents,
                documentTypes
        );
        
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }
    
    /**
     * PUT /api/users/profile — Simple JSON profile update (no file uploads).
     */
    @PutMapping(value = "/profile", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileJson(
            Authentication authentication,
            @RequestBody com.medsyncpro.dto.UpdateProfileRequest request) {
        
        String userId = utils.getUserFromAuth(authentication).getId();
        ProfileResponse updatedProfile = profileService.simpleUpdateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }
    
    @GetMapping("/me/verification-status")
    public ResponseEntity<ApiResponse<com.medsyncpro.dto.VerificationStatusResponse>> getVerificationStatus(Authentication authentication) {
        String userId = utils.getUserFromAuth(authentication).getId();
        com.medsyncpro.dto.VerificationStatusResponse status = profileService.getVerificationStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status, "Verification status retrieved"));
    }
    
    /**
     * GET /api/users/me/required-documents — get the required documents checklist with upload status.
     */
    @GetMapping("/me/required-documents")
    public ResponseEntity<ApiResponse<List<RequiredDocumentItem>>> getRequiredDocuments(Authentication authentication) {
        String userId = utils.getUserFromAuth(authentication).getId();
        List<RequiredDocumentItem> docs = profileService.getRequiredDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success(docs, "Required documents list retrieved"));
    }
    
    /**
     * POST /api/users/me/documents/{type} — upload a single document by type (replaces if exists).
     */
    @PostMapping(value = "/me/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.medsyncpro.dto.VerificationStatusResponse>> uploadSingleDocument(
            Authentication authentication,
            @PathVariable String type,
            @RequestPart("file") MultipartFile file) {
        
        String userId = utils.getUserFromAuth(authentication).getId();
        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_DOCUMENT_TYPE", "Invalid document type: " + type);
        }
        
        com.medsyncpro.dto.VerificationStatusResponse status = profileService.uploadSingleDocument(userId, docType, file);
        return ResponseEntity.ok(ApiResponse.success(status, "Document uploaded successfully"));
    }
    
    /**
     * DELETE /api/users/me/documents/{type} — delete a document by type.
     */
    @DeleteMapping("/me/documents/{type}")
    public ResponseEntity<ApiResponse<com.medsyncpro.dto.VerificationStatusResponse>> deleteSingleDocument(
            Authentication authentication,
            @PathVariable String type) {
        
        String userId = utils.getUserFromAuth(authentication).getId();
        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_DOCUMENT_TYPE", "Invalid document type: " + type);
        }
        
        com.medsyncpro.dto.VerificationStatusResponse status = profileService.deleteSingleDocument(userId, docType);
        return ResponseEntity.ok(ApiResponse.success(status, "Document deleted successfully"));
    }
    
    /**
     * POST /api/users/me/submit-verification — submit for admin review.
     */
    @PostMapping("/me/submit-verification")
    public ResponseEntity<ApiResponse<com.medsyncpro.dto.VerificationStatusResponse>> submitForVerification(Authentication authentication) {
        String userId = utils.getUserFromAuth(authentication).getId();
        com.medsyncpro.dto.VerificationStatusResponse status = profileService.submitForVerification(userId);
        return ResponseEntity.ok(ApiResponse.success(status, "Verification submitted successfully. Our team will review your documents."));
    }
    
    // ── Legacy batch upload (kept for backward compatibility) ──
    
    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.medsyncpro.dto.VerificationStatusResponse>> uploadDocuments(
            Authentication authentication,
            @RequestPart("documents") List<MultipartFile> documents,
            @RequestParam Map<String, String> documentTypes) {
        
        String userId = utils.getUserFromAuth(authentication).getId();
        com.medsyncpro.dto.VerificationStatusResponse status = profileService.uploadVerificationDocuments(userId, documents, documentTypes);
        return ResponseEntity.ok(ApiResponse.success(status, "Documents uploaded successfully"));
    }
}
