package com.medsyncpro.controller;

import com.medsyncpro.dto.response.DocumentTypeConfigResponse;
import com.medsyncpro.dto.response.ProfileResponse;
import com.medsyncpro.dto.response.RequiredDocumentItem;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.UserModelType;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.medsyncpro.dto.response.LoginResponse;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    // private final DocumentTypeService documentTypeService;
    private final Utils utils;

    /**
     * Lightweight session validation endpoint.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(Authentication authentication) {
        User user = utils.getUserFromAuth(authentication);
        LoginResponse response = LoginResponse.builder().email(user.getEmail()).role(user.getRole()).build();
        return ResponseEntity.ok(ApiResponse.success(response, "Session valid"));
    }

    // @GetMapping("/profile")
    // public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     ProfileResponse profile = profileService.getProfile(userId);
    //     return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    // }

    // @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
    //         Authentication authentication,
    //         @RequestPart(value = "profile", required = false) String profileJson,
    //         @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
    //         @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
    //         @RequestParam(required = false) Map<String, String> documentTypes) {

    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());

    //     ProfileResponse updatedProfile = profileService.updateProfile(
    //             userId,
    //             profileJson,
    //             profileImage,
    //             documents,
    //             documentTypes);

    //     return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    // }

    /**
     * PUT /api/users/profile — Simple JSON profile update (no file uploads).
     */
    // @PutMapping(value = "/profile", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    // public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileJson(
    //         Authentication authentication,
    //         @RequestBody com.medsyncpro.dto.request.UpdateProfileRequest request) {

    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     ProfileResponse updatedProfile = profileService.simpleUpdateProfile(userId, request);
    //     return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    // }

    // @GetMapping("/me/verification-status")
    // public ResponseEntity<ApiResponse<com.medsyncpro.dto.response.VerificationStatusResponse>> getVerificationStatus(
    //         Authentication authentication) {
    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     com.medsyncpro.dto.response.VerificationStatusResponse status = profileService.getVerificationStatus(userId);
    //     return ResponseEntity.ok(ApiResponse.success(status, "Verification status retrieved"));
    // }

    /**
     * GET /api/users/me/required-documents — get the required documents checklist
     * with upload status.
     */
    // @GetMapping("/me/required-documents")
    // public ResponseEntity<ApiResponse<List<RequiredDocumentItem>>> getRequiredDocuments(Authentication authentication) {
    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     List<RequiredDocumentItem> docs = profileService.getRequiredDocuments(userId);
    //     return ResponseEntity.ok(ApiResponse.success(docs, "Required documents list retrieved"));
    // }

    /**
     * GET /api/users/me/document-types — returns active document types for the
     * authenticated user's model.
     */
    // @GetMapping("/me/document-types")
    // public ResponseEntity<ApiResponse<List<DocumentTypeConfigResponse>>> getMyDocumentTypes(
    //         Authentication authentication) {
    //     User user = utils.getUserFromAuth(authentication);
    //     UserModelType modelType = ProfileService.roleToModelType(user.getRole());
    //     if (modelType == null) {
    //         return ResponseEntity.ok(ApiResponse.success(Collections.emptyList(), "No document types for this role"));
    //     }
    //     List<DocumentTypeConfigResponse> configs = documentTypeService.getActiveDocumentTypesForModel(modelType);
    //     return ResponseEntity.ok(ApiResponse.success(configs, "Document types retrieved"));
    // }

    /**
     * POST /api/users/me/documents/{documentTypeId} — upload a single document by
     * type ID.
     */
    // @PostMapping(value = "/me/documents/{documentTypeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResponse<com.medsyncpro.dto.response.VerificationStatusResponse>> uploadSingleDocument(
    //         Authentication authentication,
    //         @PathVariable Long documentTypeId,
    //         @RequestPart("file") MultipartFile file) {

    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());

    //     com.medsyncpro.dto.response.VerificationStatusResponse status = profileService.uploadSingleDocument(userId,
    //             documentTypeId, file);
    //     return ResponseEntity.ok(ApiResponse.success(status, "Document uploaded successfully"));
    // }

    /**
     * DELETE /api/users/me/documents/{documentTypeId} — delete a document by type
     * ID.
     */
    // @DeleteMapping("/me/documents/{documentTypeId}")
    // public ResponseEntity<ApiResponse<com.medsyncpro.dto.response.VerificationStatusResponse>> deleteSingleDocument(
    //         Authentication authentication,
    //         @PathVariable Long documentTypeId) {

    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());

    //     com.medsyncpro.dto.response.VerificationStatusResponse status = profileService.deleteSingleDocument(userId,
    //             documentTypeId);
    //     return ResponseEntity.ok(ApiResponse.success(status, "Document deleted successfully"));
    // }

    /**
     * POST /api/users/me/submit-verification — submit for admin review.
     */
    // @PostMapping("/me/submit-verification")
    // public ResponseEntity<ApiResponse<com.medsyncpro.dto.response.VerificationStatusResponse>> submitForVerification(
    //         Authentication authentication) {
    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     com.medsyncpro.dto.response.VerificationStatusResponse status = profileService.submitForVerification(userId);
    //     return ResponseEntity.ok(ApiResponse.success(status,
    //             "Verification submitted successfully. Our team will review your documents."));
    // }

    // ── Legacy batch upload (kept for backward compatibility) ──

    // @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResponse<com.medsyncpro.dto.response.VerificationStatusResponse>> uploadDocuments(
    //         Authentication authentication,
    //         @RequestPart("documents") List<MultipartFile> documents,
    //         @RequestParam Map<String, String> documentTypes) {

    //     String userId = String.valueOf(utils.getUserFromAuth(authentication).getId());
    //     com.medsyncpro.dto.response.VerificationStatusResponse status = profileService
    //             .uploadVerificationDocuments(userId, documents, documentTypes);
    //     return ResponseEntity.ok(ApiResponse.success(status, "Documents uploaded successfully"));
    // }
}
