package com.medsyncpro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.medsyncpro.dto.request.LoginRequest;
import com.medsyncpro.dto.response.LoginResponse;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.access-expiration:900000}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;


    public ApiResponse<LoginResponse> login(LoginRequest request,
        HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        User user = userRepository.findByEmail(request.getEmail());
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
}
