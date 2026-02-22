package com.medsyncpro.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;

@Service
@Primary
@Slf4j
public class CloudinaryStorageService implements FileStorageService {
    
    private final Cloudinary cloudinary;
    
    private static final String[] ALLOWED_IMAGE_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };
    
    private static final String[] ALLOWED_DOCUMENT_TYPES = {
        "application/pdf", "image/jpeg", "image/jpg", "image/png",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };
    
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;
    
    public CloudinaryStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
    }
    
    @Override
    public String uploadProfileImage(MultipartFile file, Long userId) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        return uploadToCloudinary(file, "medsyncpro/profiles/" + userId);
    }
    
    @Override
    public String uploadDocument(MultipartFile file, Long userId) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE);
        return uploadToCloudinary(file, "medsyncpro/documents/" + userId);
    }
    
    @Override
    public void deleteFile(String url) {
        try {
            String publicId = extractPublicId(url);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted file from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", url, e);
        }
    }
    
    @Override
    public void validateFile(MultipartFile file, String[] allowedTypes, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "File is empty");
        }
        
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("FILE_TOO_LARGE", 
                "File size exceeds maximum allowed size of " + (maxSizeBytes / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE", 
                "Invalid file type. Allowed types: " + String.join(", ", allowedTypes));
        }
    }
    
    private String uploadToCloudinary(MultipartFile file, String folder) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
                ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
                ));
            
            String url = (String) uploadResult.get("secure_url");
            log.info("Uploaded file to Cloudinary: {}", url);
            return url;
            
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BusinessException("FILE_UPLOAD_FAILED", "Failed to upload file to cloud storage");
        }
    }
    
    private String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;
            
            String afterUpload = parts[1];
            int versionIndex = afterUpload.indexOf("/v");
            if (versionIndex > 0) {
                afterUpload = afterUpload.substring(versionIndex + 1);
            }
            
            int slashIndex = afterUpload.indexOf("/");
            if (slashIndex > 0) {
                afterUpload = afterUpload.substring(slashIndex + 1);
            }
            
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex > 0) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }
            
            return afterUpload;
        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", url, e);
            return null;
        }
    }
}
