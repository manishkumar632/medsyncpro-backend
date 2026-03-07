package com.medsyncpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the client so it can upload a file directly to Cloudinary
 * without ever seeing the API secret.
 *
 * Flow:
 * 1. Client calls POST /api/{role}/documents/signature
 * 2. Server generates a time-limited signed payload and returns this DTO
 * 3. Client POSTs the file to:
 * https://api.cloudinary.com/v1_1/{cloudName}/auto/upload
 * with fields: file, signature, timestamp, api_key, folder
 * 4. On success, client calls POST /api/{role}/documents/upload with the
 * returned Cloudinary metadata (publicId, secureUrl, …)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignatureResponseDTO {

    /** Hex-encoded SHA-1 signature of the parameter string + API secret. */
    private String signature;

    /** Unix epoch seconds — must match what was signed (±10 min window). */
    private long timestamp;

    /** Cloudinary API key (public — safe to send to browser). */
    private String apiKey;

    /** Cloudinary cloud name (e.g. "dhopew3ev"). */
    private String cloudName;

    /**
     * Target folder inside your Cloudinary account (e.g.
     * "medsyncpro/docs/doctors/…").
     */
    private String folder;
}