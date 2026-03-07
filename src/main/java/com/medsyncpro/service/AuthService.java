package com.medsyncpro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medsyncpro.dto.request.LoginRequest;
import com.medsyncpro.dto.request.RegisterRequest;
import com.medsyncpro.dto.response.LoginResponse;
import com.medsyncpro.dto.response.RegisterResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.event.UserSignupEvent;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import com.medsyncpro.response.ApiResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final VerificationService verificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PharmacyRepository pharmacyRepository;
    private final AgentRepository agentRepository;
    private final AuditLogService auditLogService;

    @Value("${jwt.access-expiration:900000}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${registration.spam.limit:3}")
    private int spamLimit;

    public ApiResponse<LoginResponse> login(LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user == null || user.getDeleted() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new BusinessException("EMAIL_NOT_VERIFIED", "Please verify your email before logging in");
        }

        String deviceInfo = extractDeviceInfo(httpRequest);

        String accessToken = jwtService.generateAccessToken(user, deviceInfo);
        String refreshToken = refreshTokenService.createRefreshToken(user, deviceInfo);

        addAccessTokenCookie(httpResponse, accessToken);
        addRefreshTokenCookie(httpResponse, refreshToken);

        auditLogService.logAction(user.getId(), "LOGIN", "Login successful from device: " + deviceInfo);

        LoginResponse response = LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .phone(user.getPhone())
                .termsAccepted(user.isTermsAccepted())
                .build();

        return ApiResponse.success(response, "Login successful");
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIp(request);
        return userAgent + " | IP: " + ip;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie
                .from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(refreshExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "password and confirm password are not same");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new BusinessException("ADMIN_REGISTRATION_DISABLED", "Admin registration is not allowed");
        }

        if (!request.isTermsAccepted()) {
            throw new BusinessException("TERMS_AND_CONDITION_NOT_ACCEPTED", "Please accept terms and condition");
        }

        long recentCount = userRepository.countRecentRegistrationsByEmail(
                request.getEmail(),
                LocalDateTime.now().minusHours(1));
        if (recentCount >= spamLimit) {
            throw new BusinessException("SPAM_DETECTED", "Too many registration attempts. Please try later");
        }

        if (userRepository.existsByEmailAndNotDeleted(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "User with the email already exist. Please login");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .emailVerified(false)
                .phoneVerified(false)
                .termsAccepted(true)
                .build();

        try {
            user = userRepository.save(user);

            createRoleBasedEntity(user, request.getName());

            verificationService.generateAndSendToken(user);

            if (user.getRole() == Role.DOCTOR || user.getRole() == Role.PHARMACY || user.getRole() == Role.AGENT) {
                eventPublisher.publishEvent(new UserSignupEvent(this, user));
            }

            auditLogService.logAction(user.getId(), "SIGNUP", "Signed up with role: " + user.getRole());

            return RegisterResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole())
                    .emailVerified(user.isEmailVerified())
                    .build();
        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation during registration for email: {}", request.getEmail(), e);
            throw new BusinessException("DUPLICATE_ENTRY",
                    "Email already exists. Please use a different email or try logging in");
        }
    }

    private void createRoleBasedEntity(User user, String name) {
        switch (user.getRole()) {
            case DOCTOR:
                Doctor doctor = Doctor.builder()
                        .user(user)
                        .name(name)
                        .isVerified(false)
                        .build();
                doctorRepository.save(doctor);
                break;

            case PATIENT:
                Patient patient = Patient.builder()
                        .user(user)
                        .name(name)
                        .build();
                patientRepository.save(patient);
                break;

            case PHARMACY:
                Pharmacy pharmacy = Pharmacy.builder()
                        .user(user)
                        .name(name)
                        .isVerified(false)
                        .build();
                pharmacyRepository.save(pharmacy);
                break;

            case AGENT:
                Agent agent = Agent.builder()
                        .user(user)
                        .name(name)
                        .isVerified(false)
                        .build();
                agentRepository.save(agent);
                break;

            case ADMIN:
                break;
        }
    }

    public ApiResponse<LoginResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = extractCookieValue(request, "refresh_token");

        if (refreshTokenStr == null) {
            throw new BusinessException(
                    "MISSING_REFRESH_TOKEN",
                    "Refresh token cookie is missing. Please login again");
        }

        User user = refreshTokenService.validateAndRotate(refreshTokenStr);
        String deviceInfo = extractDeviceInfo(request);

        String newAccessToken = jwtService.generateAccessToken(user, deviceInfo);
        String newRefreshToken = refreshTokenService.createRefreshToken(user, deviceInfo);

        addAccessTokenCookie(response, newAccessToken);
        addRefreshTokenCookie(response, newRefreshToken);

        LoginResponse loginResponse = LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .phone(user.getPhone())
                .build();

        return ApiResponse.success(loginResponse, "Token refreshed successfully");
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return (value != null && !value.isBlank()) ? value : null;
                }
            }
        }
        return null;
    }
}
