package com.medsyncpro.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.medsyncpro.dto.response.SignatureResponseDTO;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
@Slf4j
public class CloudinaryStorageService implements FileStorageService {

    private final Cloudinary cloudinary;

    /** Stored separately so we can embed them in the signature response. */
    private final String apiKey;
    private final String cloudName;
    private final String apiSecret;

    // ─── Allowed MIME types ──────────────────────────────────────────────────

    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    private static final String[] ALLOWED_DOCUMENT_TYPES = {
            "application/pdf",
            "image/jpeg", "image/jpg", "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    // ─── Size limits ─────────────────────────────────────────────────────────

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10 MB

    // ─── Constructor ─────────────────────────────────────────────────────────

    public CloudinaryStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    // ─── Server-side uploads ─────────────────────────────────────────────────

    @Override
    public String uploadProfileImage(MultipartFile file, String userId) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        return uploadToCloudinary(file, "medsyncpro/profiles/" + userId);
    }

    @Override
    public String uploadDocument(MultipartFile file, String userId) {
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
                    "File size exceeds the maximum of " + (maxSizeBytes / 1024 / 1024) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE",
                    "Invalid file type. Allowed: " + String.join(", ", allowedTypes));
        }
    }

    // ─── Client-side signed upload ───────────────────────────────────────────

    /**
     * Generates a Cloudinary upload signature so the browser can POST a file
     * directly to Cloudinary without the file ever touching this server.
     *
     * <p>
     * Cloudinary validates the signature with:
     * 
     * <pre>
     * SHA1("folder=X&timestamp=Y" + API_SECRET)
     * </pre>
     * 
     * Parameters must be sorted alphabetically before hashing.
     *
     * @param folder destination folder inside the Cloudinary account
     * @return a {@link SignatureResponseDTO} the browser uses for its upload POST
     */
    @Override
    public SignatureResponseDTO generateUploadSignature(String folder) {
        try {
            long timestamp = System.currentTimeMillis() / 1000L;

            // Parameters that will be included in the upload POST —
            // they must be signed so Cloudinary can verify the request.
            Map<String, Object> paramsToSign = new HashMap<>();
            paramsToSign.put("folder", folder);
            paramsToSign.put("timestamp", timestamp);

            // Cloudinary SDK handles the alphabetical sort + SHA-1 hash for us.
            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);

            log.debug("Generated Cloudinary upload signature for folder: {}", folder);

            return SignatureResponseDTO.builder()
                    .signature(signature)
                    .timestamp(timestamp)
                    .apiKey(apiKey)
                    .cloudName(cloudName)
                    .folder(folder)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate Cloudinary upload signature", e);
            throw new BusinessException("SIGNATURE_FAILED",
                    "Failed to generate upload signature. Please try again.");
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String uploadToCloudinary(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"));
            String url = (String) result.get("secure_url");
            log.info("Uploaded file to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BusinessException("FILE_UPLOAD_FAILED",
                    "Failed to upload file to cloud storage");
        }
    }

    private String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com"))
            return null;
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2)
                return null;

            String after = parts[1];

            // Strip version segment (v1234567890/…)
            int vIdx = after.indexOf("/v");
            if (vIdx > 0)
                after = after.substring(vIdx + 1);

            int slashIdx = after.indexOf("/");
            if (slashIdx > 0)
                after = after.substring(slashIdx + 1);

            int dotIdx = after.lastIndexOf(".");
            if (dotIdx > 0)
                after = after.substring(0, dotIdx);

            return after;
        } catch (Exception e) {
            log.error("Failed to extract public_id from URL: {}", url, e);
            return null;
        }
    }
}