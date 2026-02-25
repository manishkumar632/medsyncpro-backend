package com.medsyncpro.controller;

import com.medsyncpro.dto.LoginRequest;
import com.medsyncpro.dto.LoginResponse;
import com.medsyncpro.dto.RegisterRequest;
import com.medsyncpro.entity.RefreshToken;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.*;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;
    private final VerificationService verificationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @Value("${jwt.access-expiration:900000}")
    private long accessExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;
    
    // ==================== REGISTER ====================
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully. Please verify your email"));
    }
    
    // ==================== LOGIN ====================
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        
        LoginResponse loginResponse = userService.login(request);
        User user = userService.getUserByEmail(request.getEmail());
        String deviceInfo = extractDeviceInfo(httpRequest);
        
        // Generate access token (short-lived, 15 min)
        String accessToken = jwtService.generateAccessToken(user, deviceInfo);
        
        // Generate refresh token (long-lived, 7 days)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceInfo);
        
        // Set cookies
        addAccessTokenCookie(response, accessToken);
        addRefreshTokenCookie(response, refreshToken.getToken());
        
        log.info("User {} logged in from device: {}", user.getEmail(), deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful"));
    }
    
    // ==================== REFRESH TOKEN ====================
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 1. Extract refresh token from cookie
        String refreshTokenStr = extractCookieValue(request, "refresh_token");
        if (refreshTokenStr == null) {
            throw new BusinessException("MISSING_REFRESH_TOKEN", "Refresh token cookie is missing. Please login again");
        }
        
        // 2. Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenStr);
        User user = refreshToken.getUser();
        String deviceInfo = refreshToken.getDeviceInfo();
        
        // 3. Rotate refresh token (revoke old, create new)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);
        
        // 4. Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user, deviceInfo);
        
        // 5. Set new cookies
        addAccessTokenCookie(response, newAccessToken);
        addRefreshTokenCookie(response, newRefreshToken.getToken());
        
        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getEmail(), user.getName(), user.getRole());
        
        log.info("Token refreshed for user {} on device {}", user.getEmail(), deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Token refreshed successfully"));
    }
    
    // ==================== LOGOUT (Current Device) ====================
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String accessToken = extractCookieValue(request, "access_token");
        String refreshTokenStr = extractCookieValue(request, "refresh_token");
        
        // Blacklist access token if present (even if expired)
        if (accessToken != null) {
            try {
                Claims claims = jwtService.extractClaimsAllowExpired(accessToken);
                String jti = jwtService.extractJti(claims);
                Instant expiry = jwtService.extractExpiration(claims).toInstant();
                String email = jwtService.extractEmail(claims);
                String deviceInfo = jwtService.extractDeviceInfo(claims);
                
                // Blacklist the access token
                if (jti != null) {
                    tokenBlacklistService.blacklist(jti, expiry);
                }
                
                // Revoke refresh token for this device
                if (email != null && deviceInfo != null) {
                    User user = userService.getUserByEmail(email);
                    if (user != null) {
                        refreshTokenService.revokeByDevice(user, deviceInfo);
                    }
                }
                
                log.info("User {} logged out from device {}", email, deviceInfo);
            } catch (Exception e) {
                log.warn("Error processing access token during logout: {}", e.getMessage());
            }
        }
        
        // Also try to revoke the refresh token directly
        if (refreshTokenStr != null) {
            try {
                RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenStr);
                refreshTokenService.revokeByDevice(refreshToken.getUser(), refreshToken.getDeviceInfo());
            } catch (Exception e) {
                // Refresh token already revoked or expired — that's fine (idempotent)
            }
        }
        
        // Always clear cookies (idempotent — works even if already logged out)
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
    
    // ==================== LOGOUT ALL DEVICES ====================
    
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Object>> logoutAll(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String accessToken = extractCookieValue(request, "access_token");
        
        // Need to identify the user — try from Authentication first, then from token
        User user = null;
        
        if (authentication != null && authentication.getName() != null) {
            user = userService.getUserByEmail(authentication.getName());
        }
        
        if (user == null && accessToken != null) {
            try {
                Claims claims = jwtService.extractClaimsAllowExpired(accessToken);
                String email = jwtService.extractEmail(claims);
                if (email != null) {
                    user = userService.getUserByEmail(email);
                }
            } catch (Exception e) {
                log.warn("Could not extract user from token: {}", e.getMessage());
            }
        }
        
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "Unable to identify user. Please login again");
        }
        
        // 1. Blacklist current access token
        if (accessToken != null) {
            try {
                Claims claims = jwtService.extractClaimsAllowExpired(accessToken);
                String jti = jwtService.extractJti(claims);
                Instant expiry = jwtService.extractExpiration(claims).toInstant();
                if (jti != null) {
                    tokenBlacklistService.blacklist(jti, expiry);
                }
            } catch (Exception e) {
                log.warn("Error blacklisting access token: {}", e.getMessage());
            }
        }
        
        // 2. Revoke ALL refresh tokens
        refreshTokenService.revokeAllForUser(user);
        
        // 3. Increment token version — instantly invalidates all access tokens
        userService.incrementTokenVersion(user);
        
        // 4. Clear cookies on this device
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
        
        log.info("User {} logged out from ALL devices", user.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices successfully"));
    }
    
    // ==================== EMAIL VERIFICATION ====================
    
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody com.medsyncpro.dto.VerifyEmailRequest request) {
        verificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(@RequestParam String email) {
        verificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
    }
    
    // ==================== HELPERS ====================
    
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
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
    
    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(refreshExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void clearAccessTokenCookie(HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
