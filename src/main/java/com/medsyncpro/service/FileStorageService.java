package com.medsyncpro.service;

import com.medsyncpro.dto.response.SignatureResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for file-storage operations.
 * Implementations can use S3, Cloudinary, Firebase Storage, etc.
 *
 * Two upload strategies are supported:
 * • Server-side — browser sends file to Spring Boot → Spring Boot uploads
 * • Client-side — browser uploads directly to the storage provider using
 * a time-limited signature obtained from generateUploadSignature()
 *
 * The signed-upload strategy is preferred for large files or high-traffic
 * scenarios because the file bytes never pass through the application server.
 */
public interface FileStorageService {

    // ─── Server-side uploads ─────────────────────────────────────────────────

    /**
     * Upload a profile image and return its public URL.
     *
     * @param file   the image file
     * @param userId user identifier used to organise files in storage
     * @return public (CDN) URL of the uploaded image
     */
    String uploadProfileImage(MultipartFile file, String userId);

    /**
     * Upload a document and return its public URL.
     *
     * @param file   the document file
     * @param userId user identifier used to organise files in storage
     * @return public (CDN) URL of the uploaded document
     */
    String uploadDocument(MultipartFile file, String userId);

    /**
     * Delete a file from storage by its URL.
     *
     * @param url the full public URL of the file to delete
     */
    void deleteFile(String url);

    /**
     * Validate file type and size.
     *
     * @param file         the file to validate
     * @param allowedTypes allowed MIME types
     * @param maxSizeBytes maximum file size in bytes
     */
    void validateFile(MultipartFile file, String[] allowedTypes, long maxSizeBytes);

    // ─── Client-side (signed) uploads ────────────────────────────────────────

    /**
     * Generate a time-limited Cloudinary upload signature for a browser-to-CDN
     * direct upload.
     *
     * <p>
     * The caller assembles a multipart POST to Cloudinary using:
     * 
     * <pre>
     *   POST https://api.cloudinary.com/v1_1/{cloudName}/auto/upload
     *   Body: file=&lt;bytes&gt;, signature=…, timestamp=…, api_key=…, folder=…
     * </pre>
     *
     * @param folder Cloudinary destination folder
     *               (e.g. "medsyncpro/docs/doctors/&lt;userId&gt;")
     * @return {@link SignatureResponseDTO} containing everything the browser needs
     */
    SignatureResponseDTO generateUploadSignature(String folder);
}