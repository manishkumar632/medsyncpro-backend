package com.medsyncpro.filter;

import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.service.JwtService;
import com.medsyncpro.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = extractTokenFromCookie(request, "access_token");
        
        // No token = anonymous request, continue to let Spring Security handle authorization
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 1. Parse and validate JWT (handles invalid, tampered, wrong signature, random tokens)
            Claims claims = jwtService.extractClaims(token);
            
            // 2. Check if token is blacklisted
            String jti = jwtService.extractJti(claims);
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }
            
            // 3. Validate token version (handles logout-all-devices)
            String email = jwtService.extractEmail(claims);
            Integer tokenVersion = jwtService.extractTokenVersion(claims);
            
            if (email != null) {
                User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (user == null || user.getDeleted()) {
                    sendUnauthorizedResponse(response, "User not found or deleted");
                    return;
                }
                
                if (tokenVersion != null && !tokenVersion.equals(user.getTokenVersion())) {
                    sendUnauthorizedResponse(response, "Token version mismatch. Please login again");
                    return;
                }
                
                // 4. All checks passed — set authentication
                String role = claims.get("role", String.class);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            
        } catch (ExpiredJwtException e) {
            sendUnauthorizedResponse(response, "Token has expired");
            return;
        } catch (SignatureException e) {
            sendUnauthorizedResponse(response, "Invalid token signature");
            return;
        } catch (MalformedJwtException e) {
            sendUnauthorizedResponse(response, "Malformed token");
            return;
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Invalid token");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Send a proper 401 JSON response instead of an empty response.
     * This handles the "logout while request in progress" edge case —
     * any in-flight API call gets a clean 401 with a message.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"success\":false,\"message\":\"" + message + "\",\"data\":null,\"errors\":null}"
        );
    }
}
