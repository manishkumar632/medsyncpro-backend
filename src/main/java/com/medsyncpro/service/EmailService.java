package com.medsyncpro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    // ─── Email-address verification (existing) ────────────────────────────────

    @Async
    public void sendVerificationEmail(String email, String token) {
        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto">
                  <h2 style="color:#0d9488">Verify your email — MedSyncPro</h2>
                  <p>Click the button below to verify your email address.
                     This link expires in 24 hours.</p>
                  <a href="%s/auth/verify-email?token=%s"
                     style="display:inline-block;background:#0d9488;color:#fff;
                            padding:12px 24px;border-radius:8px;text-decoration:none;
                            font-weight:600">
                    Verify Email
                  </a>
                </div>
                """.formatted(frontendUrl, token);

        send(email, "Verify Your Email - MedSyncPro", html);
    }

    // ─── Verification approved ────────────────────────────────────────────────

    @Async
    public void sendVerificationApprovedEmail(String email, String name) {
        String displayName = (name != null && !name.isBlank()) ? name : "Doctor";
        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto">
                  <h2 style="color:#0d9488">🎉 Verification Approved — MedSyncPro</h2>
                  <p>Dear %s,</p>
                  <p>
                    Great news! Your professional documents have been reviewed and
                    <strong style="color:#0d9488">approved</strong> by the MedSyncPro team.
                    Your account is now fully verified and you can access all platform features.
                  </p>
                  <a href="%s/doctor/dashboard"
                     style="display:inline-block;background:#0d9488;color:#fff;
                            padding:12px 24px;border-radius:8px;text-decoration:none;
                            font-weight:600">
                    Go to Dashboard
                  </a>
                  <p style="margin-top:24px;color:#64748b;font-size:13px">
                    If you have any questions, contact our support team.
                  </p>
                </div>
                """.formatted(displayName, frontendUrl);

        send(email, "✅ Your MedSyncPro Verification Has Been Approved", html);
    }

    // ─── Verification rejected ────────────────────────────────────────────────
    @Async
    public void sendVerificationRejectedEmail(String email, String name, String reason) {
        String displayName = (name != null && !name.isBlank()) ? name : "Doctor";
        String reasonText = (reason != null && !reason.isBlank()) ? reason : "No specific reason was provided.";

        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto">
                  <h2 style="color:#dc2626">❌ Verification Rejected — MedSyncPro</h2>
                  <p>Dear %s,</p>
                  <p>
                    Unfortunately, your verification request has been
                    <strong style="color:#dc2626">rejected</strong>.
                  </p>
                  <div style="background:#fef2f2;border-left:4px solid #dc2626;
                              padding:12px 16px;border-radius:4px;margin:16px 0">
                    <strong>Reason:</strong><br/>%s
                  </div>
                  <p>
                    Please review the feedback, update your documents, and re-submit
                    for verification from your dashboard.
                  </p>
                  <a href="%s/doctor/verification"
                     style="display:inline-block;background:#dc2626;color:#fff;
                            padding:12px 24px;border-radius:8px;text-decoration:none;
                            font-weight:600">
                    Re-submit Documents
                  </a>
                </div>
                """.formatted(displayName, reasonText, frontendUrl);

        send(email, "❌ Your MedSyncPro Verification Was Rejected", html);
    }

    // ─── Document re-upload request ───────────────────────────────────────────

    @Async
    public void sendResubmitRequestEmail(String email,
            String name,
            List<String> documentTypeNames,
            String comment) {
        String displayName = (name != null && !name.isBlank()) ? name : "Doctor";

        // Build a bullet list of the documents that need re-uploading
        String docList = documentTypeNames.stream()
                .map(d -> "<li style='margin:4px 0'>📄 " + d + "</li>")
                .collect(Collectors.joining("\n", "<ul>", "</ul>"));

        String commentSection = (comment != null && !comment.isBlank())
                ? """
                        <div style="background:#fefce8;border-left:4px solid #ca8a04;
                                    padding:12px 16px;border-radius:4px;margin:16px 0">
                          <strong>Admin note:</strong><br/>%s
                        </div>
                        """.formatted(comment)
                : "";

        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto">
                  <h2 style="color:#ca8a04">⚠️ Action Required — Document Re-upload</h2>
                  <p>Dear %s,</p>
                  <p>
                    Our review team has requested that you re-upload the following
                    document(s) before your verification can proceed:
                  </p>
                  %s
                  %s
                  <p>
                    Please log in to your dashboard, upload the updated documents,
                    and re-submit for verification.
                  </p>
                  <a href="%s/doctor/verification"
                     style="display:inline-block;background:#ca8a04;color:#fff;
                            padding:12px 24px;border-radius:8px;text-decoration:none;
                            font-weight:600">
                    Upload Documents
                  </a>
                  <p style="margin-top:24px;color:#64748b;font-size:13px">
                    If you believe this is an error, please contact support.
                  </p>
                </div>
                """.formatted(displayName, docList, commentSection, frontendUrl);

        send(email, "⚠️ MedSyncPro — Please Re-upload Your Documents", html);
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    @Async
    public void sendGenericNotificationEmail(String email, String displayName, String subject, String message) {
        String safeName = (displayName != null && !displayName.isBlank()) ? displayName : "User";
        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto">
                  <h2 style="color:#0d9488">%s</h2>
                  <p>Hi %s,</p>
                  <p>%s</p>
                  <p style="margin-top:24px;color:#64748b;font-size:13px">
                    This is an automated update from MedSyncPro.
                  </p>
                </div>
                """.formatted(subject, safeName, message);
        send(email, subject, html);
    }
    private void send(String toEmail, String subject, String htmlContent) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            log.info("[Email] Sent '{}' → {}", subject, toEmail);
        } catch (Exception e) {
            log.error("[Email] Failed to send '{}' → {}: {}", subject, toEmail, e.getMessage(), e);
        }
    }
}
