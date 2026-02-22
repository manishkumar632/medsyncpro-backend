package com.medsyncpro.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for file storage operations.
 * Implementations can use S3, Cloudinary, Firebase Storage, etc.
 */
public interface FileStorageService {
    
    /**
     * Upload profile image and return URL
     * @param file The image file
     * @param userId User identifier for organizing files
     * @return Public URL of uploaded file
     */
    String uploadProfileImage(MultipartFile file, Long userId);
    
    /**
     * Upload document and return URL
     * @param file The document file
     * @param userId User identifier for organizing files
     * @return Public URL of uploaded file
     */
    String uploadDocument(MultipartFile file, Long userId);
    
    /**
     * Delete file from storage
     * @param url The file URL to delete
     */
    void deleteFile(String url);
    
    /**
     * Validate file type and size
     * @param file The file to validate
     * @param allowedTypes Allowed MIME types
     * @param maxSizeBytes Maximum file size in bytes
     */
    void validateFile(MultipartFile file, String[] allowedTypes, long maxSizeBytes);
}
