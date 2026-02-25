package com.medsyncpro.controller;

import com.medsyncpro.dto.LoginResponse;
import com.medsyncpro.dto.ProfileResponse;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    private final UserRepository userRepository;
    
    
    /**
     * Lightweight session validation endpoint.
     * Returns basic user info for the currently authenticated user.
     * All security checks (expired token, blacklisted, deleted user, token version)
     * are handled by JwtAuthenticationFilter before this endpoint is reached.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        LoginResponse response = new LoginResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
        return ResponseEntity.ok(ApiResponse.success(response, "Session valid"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        String userId = getUserFromAuth(authentication).getId();
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
        
        String userId = getUserFromAuth(authentication).getId();
        
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
     * This matches the frontend PatientProfilePage which sends PUT + JSON.
     */
    @PutMapping(value = "/profile", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileJson(
            Authentication authentication,
            @RequestBody com.medsyncpro.dto.UpdateProfileRequest request) {
        
        String userId = getUserFromAuth(authentication).getId();
        ProfileResponse updatedProfile = profileService.simpleUpdateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }
    
    private User getUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
        
        return user;
    }
}
