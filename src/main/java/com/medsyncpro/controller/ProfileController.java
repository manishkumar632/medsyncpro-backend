package com.medsyncpro.controller;

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
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
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
        
        Long userId = getUserIdFromAuth(authentication);
        
        ProfileResponse updatedProfile = profileService.updateProfile(
                userId,
                profileJson,
                profileImage,
                documents,
                documentTypes
        );
        
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }
    
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
        
        return user.getId();
    }
}
